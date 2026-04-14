package br.com.tcf5_health_record_transformer.messaging;

import br.com.tcf5_health_record_transformer.domain.model.HealthRecord;
import br.com.tcf5_health_record_transformer.domain.service.JoltTransformer;
import br.com.tcf5_health_record_transformer.domain.service.RuleService;
import br.com.tcf5_health_record_transformer.infrastructure.repository.HealthRecordRepository;
import br.com.tcf5_health_record_transformer.validation.FhirValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class HealthRecordConsumer {

    private static final Logger log = LoggerFactory.getLogger(HealthRecordConsumer.class);

    // Executor agendado para retries de envio ao DLQ (daemon)
    private static final ScheduledExecutorService DLQ_SCHEDULER = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "dlq-retry-thread");
        t.setDaemon(true);
        return t;
    });

    private final RuleService ruleService;
    private final JoltTransformer transformer;
    private final HealthRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FhirValidationService fhirValidationService;
    private final boolean fhirValidationEnabled;

    private final String dlqTopic;

    public HealthRecordConsumer(RuleService ruleService, JoltTransformer transformer, HealthRecordRepository repository, ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate, FhirValidationService fhirValidationService, @Value("${app.kafka.dlq-topic}") String dlqTopic, @Value("${app.fhir.validation.enabled:true}") boolean fhirValidationEnabled) {
        this.ruleService = ruleService;
        this.transformer = transformer;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.fhirValidationService = fhirValidationService;
        this.dlqTopic = dlqTopic;
        this.fhirValidationEnabled = fhirValidationEnabled;
    }

    @KafkaListener(topics = "${app.kafka.inbound-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> consumerRecord) {
        String message = consumerRecord.value();
        try {
            log.info("Mensagem recebida do Kafka");
            JsonNode root = objectMapper.readTree(message);

            String clientIdStr = null;
            if (consumerRecord.headers() != null) {
                var h = consumerRecord.headers().lastHeader("clientId");
                if (h != null && h.value() != null) {
                    clientIdStr = new String(h.value(), StandardCharsets.UTF_8).trim();
                }
            }
            String patientId = root.path("paciente").path("documento").asText();
            if (patientId == null) {
                patientId = root.path("paciente").path("cpf").asText();
            }

            // 1. Busca Regra JOLT
            UUID clientId = parseClientId(clientIdStr);
            String spec = ruleService.getSpec(clientId);

            // 2. Transforma para FHIR (Simulado pelo JOLT)
            String fhirJson = transformer.transform(message, spec);

            // 2.1 validar FHIR JSON (usando HAPI FHIR) se habilitado
            if (fhirValidationEnabled) {
                fhirValidationService.validateOrThrow(fhirJson);
            }

            // 3. Persiste no Postgres
            HealthRecord healthRecord = new HealthRecord();
            healthRecord.setPatientId(patientId);
            healthRecord.setResourceType("Bundle");
            healthRecord.setResourceContent(fhirJson);
            healthRecord.setProcessedAt(LocalDateTime.now());
            healthRecord.setClientId(clientId);

            repository.save(healthRecord);
            log.info("Dado transformado e salvo com sucesso para o paciente: {}", patientId);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem", e);

            try {

                // Monta payload DLQ com metadados e stacktrace
                ObjectNode dlq = objectMapper.createObjectNode();
                dlq.put("originalTopic", consumerRecord.topic());
                dlq.put("originalPartition", consumerRecord.partition());
                dlq.put("originalOffset", consumerRecord.offset());
                dlq.put("originalTimestamp", consumerRecord.timestamp());

                // tenta parsear o payload original; se falhar, grava como string
                try {
                    dlq.set("payload", objectMapper.readTree(message));
                } catch (Exception exJson) {
                    dlq.put("payload_as_text", message);
                }

                dlq.put("errorMessage", e.toString());
                dlq.put("stackTrace", getStackTrace(e));
                dlq.put("processedBy", "tcf5-health-record-transformer");
                dlq.put("processedAt", LocalDateTime.now().toString());

                String dlqJson = objectMapper.writeValueAsString(dlq);

                ProducerRecord<String, String> prod = new ProducerRecord<>(dlqTopic, null, dlqJson);

                // headers para filtragem rápida
                prod.headers().add("x-error", e.toString().getBytes(StandardCharsets.UTF_8));
                prod.headers().add("x-src-topic", consumerRecord.topic().getBytes(StandardCharsets.UTF_8));
                prod.headers().add("x-src-partition", Integer.toString(consumerRecord.partition()).getBytes(StandardCharsets.UTF_8));
                prod.headers().add("x-src-offset", Long.toString(consumerRecord.offset()).getBytes(StandardCharsets.UTF_8));

                // Envia para DLQ com retries (3 tentativas, backoff inicial 500ms)
                CompletableFuture<SendResult<String, String>> sendFuture = sendWithRetries(prod, 3, 500);

                sendFuture.whenComplete((result, ex2) -> {
                    if (ex2 != null) {
                        log.error("Falha final ao enviar mensagem para DLQ após tentativas: {}", ex2.getMessage(), ex2);
                    } else if (result != null && result.getRecordMetadata() != null) {
                        RecordMetadata md = result.getRecordMetadata();
                        log.info("Mensagem enviada para DLQ {} at partition {} offset {}", dlqTopic, md.partition(), md.offset());
                    } else {
                        log.warn("Mensagem enviada para DLQ {}, mas metadados de partição/offset não estão disponíveis", dlqTopic);
                    }
                });

            } catch (Exception ex) {
                log.error("Erro ao tentar encaminhar mensagem para DLQ", ex);
            }
        }
    }

    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }
        return sw.toString();
    }

    // Envia um ProducerRecord com tentativas e backoff exponencial.
    private CompletableFuture<SendResult<String, String>> sendWithRetries(ProducerRecord<String, String> record, int maxAttempts, long initialBackoffMs) {
        CompletableFuture<SendResult<String, String>> resultFuture = new CompletableFuture<>();
        sendAttempt(record, maxAttempts, initialBackoffMs, resultFuture);
        return resultFuture;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void sendAttempt(ProducerRecord<String, String> record, int remainingAttempts, long backoffMs, CompletableFuture<SendResult<String, String>> resultFuture) {
        if (resultFuture.isDone()) {
            return;
        }

        try {
            Object sendObj = kafkaTemplate.send(record);
            CompletableFuture<SendResult<String, String>> sendCf = toCompletableFuture(sendObj);

            sendCf.whenComplete((res, ex) -> {
                if (ex == null) {
                    resultFuture.complete(res);
                } else {
                    if (remainingAttempts > 1) {
                        log.warn("Falha ao enviar para DLQ (tentativas restantes={}): {}. Reagendando em {}ms", remainingAttempts - 1, ex.getMessage(), backoffMs);
                        DLQ_SCHEDULER.schedule(() -> sendAttempt(record, remainingAttempts - 1, backoffMs * 2, resultFuture), backoffMs, TimeUnit.MILLISECONDS);
                    } else {
                        log.error("Ultima tentativa de envio para DLQ falhou: {}", ex.getMessage(), ex);
                        resultFuture.completeExceptionally(ex);
                    }
                }
            });

        } catch (Exception ex) {
            if (remainingAttempts > 1) {
                log.warn("Exceção ao tentar enviar para DLQ (tentativas restantes={}): {}. Reagendando em {}ms", remainingAttempts - 1, ex.getMessage(), backoffMs);
                DLQ_SCHEDULER.schedule(() -> sendAttempt(record, remainingAttempts - 1, backoffMs * 2, resultFuture), backoffMs, TimeUnit.MILLISECONDS);
            } else {
                resultFuture.completeExceptionally(ex);
            }
        }
    }

    // Converte um Future (possivelmente o ListenableFuture retornado por KafkaTemplate) em CompletableFuture.
    // Se for um Future genérico, faz o get() em uma thread separada para não bloquear o caller.
    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> toCompletableFuture(Object future) {
        if (future == null) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("send returned null future"));
            return failed;
        }

        if (future instanceof CompletableFuture<?> cf) {
            return (CompletableFuture<T>) cf;
        }

        if (future instanceof Future<?> f) {
            // Tenta evitar bloquear a thread atual criando uma thread que aguarda o future
            CompletableFuture<T> completable = new CompletableFuture<>();
            Thread waiter = new Thread(() -> {
                try {
                    @SuppressWarnings("unchecked")
                    T res = (T) f.get();
                    completable.complete(res);
                } catch (Exception ex) {
                    completable.completeExceptionally(ex);
                }
            });
            waiter.setDaemon(true);
            waiter.start();
            return completable;
        }

        CompletableFuture<T> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalArgumentException("Unknown future type: " + future.getClass().getName()));
        return failed;
    }

    private UUID parseClientId(String s) {
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            Throwable cause = e.getCause();
        }
        return null;
    }
}
