package br.com.tcf5_health_record_transformer.messaging;

import br.com.tcf5_health_record_transformer.domain.service.JoltTransformer;
import br.com.tcf5_health_record_transformer.domain.service.RuleService;
import br.com.tcf5_health_record_transformer.infrastructure.repository.HealthRecordRepository;
import br.com.tcf5_health_record_transformer.validation.FhirValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthRecordConsumerRetryTest {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    // proper typed mocks for constructor
    @Mock
    RuleService ruleService;
    @Mock
    JoltTransformer transformer;
    @Mock
    HealthRecordRepository repository;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    FhirValidationService fhirValidationService;

    private HealthRecordConsumer consumer;

    @BeforeEach
    void setUp() {
        // Ajuste: novo construtor aceita dlqTopic and FhirValidationService and flag
        consumer = new HealthRecordConsumer(ruleService, transformer, repository, objectMapper, kafkaTemplate, fhirValidationService, "atendimentos-brutos-dlq", true);
    }

    @Test
    void sendWithRetries_successOnFirstAttempt() throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        SendResult<String, String> successful = new SendResult<>(record, null);
        CompletableFuture<SendResult<String, String>> cf = CompletableFuture.completedFuture(successful);

        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(cf);

        Method m = HealthRecordConsumer.class.getDeclaredMethod("sendWithRetries", ProducerRecord.class, int.class, long.class);
        m.setAccessible(true);

        CompletableFuture<SendResult<String, String>> result = (CompletableFuture<SendResult<String, String>>) m.invoke(consumer, record, 3, 10L);

        SendResult<String, String> res = result.get(2, TimeUnit.SECONDS);
        assertNotNull(res);
        assertFalse(result.isCompletedExceptionally());
    }

    @Test
    void sendWithRetries_successAfterRetries() throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        CompletableFuture<SendResult<String, String>> fail = new CompletableFuture<>();
        fail.completeExceptionally(new RuntimeException("simulated failure"));

        CompletableFuture<SendResult<String, String>> success = CompletableFuture.completedFuture(new SendResult<>(record, null));

        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(fail, success);

        Method m = HealthRecordConsumer.class.getDeclaredMethod("sendWithRetries", ProducerRecord.class, int.class, long.class);
        m.setAccessible(true);

        CompletableFuture<SendResult<String, String>> result = (CompletableFuture<SendResult<String, String>>) m.invoke(consumer, record, 3, 10L);

        SendResult<String, String> res = result.get(5, TimeUnit.SECONDS);
        assertNotNull(res);
        assertFalse(result.isCompletedExceptionally());
    }

    @Test
    void sendWithRetries_failAllAttempts() throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "key", "value");

        CompletableFuture<SendResult<String, String>> fail1 = new CompletableFuture<>();
        fail1.completeExceptionally(new RuntimeException("boom1"));
        CompletableFuture<SendResult<String, String>> fail2 = new CompletableFuture<>();
        fail2.completeExceptionally(new RuntimeException("boom2"));
        CompletableFuture<SendResult<String, String>> fail3 = new CompletableFuture<>();
        fail3.completeExceptionally(new RuntimeException("boom3"));

        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(fail1, fail2, fail3);

        Method m = HealthRecordConsumer.class.getDeclaredMethod("sendWithRetries", ProducerRecord.class, int.class, long.class);
        m.setAccessible(true);

        CompletableFuture<SendResult<String, String>> result = (CompletableFuture<SendResult<String, String>>) m.invoke(consumer, record, 3, 10L);

        assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertTrue(result.isCompletedExceptionally());
    }
}
