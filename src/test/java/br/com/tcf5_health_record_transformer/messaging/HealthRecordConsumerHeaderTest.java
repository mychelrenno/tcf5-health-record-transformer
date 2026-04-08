package br.com.tcf5_health_record_transformer.messaging;

import br.com.tcf5_health_record_transformer.domain.model.HealthRecord;
import br.com.tcf5_health_record_transformer.domain.service.JoltTransformer;
import br.com.tcf5_health_record_transformer.domain.service.RuleService;
import br.com.tcf5_health_record_transformer.infrastructure.repository.HealthRecordRepository;
import br.com.tcf5_health_record_transformer.validation.FhirValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HealthRecordConsumerHeaderTest {

    @Mock
    RuleService ruleService;
    @Mock
    JoltTransformer transformer;
    @Mock
    HealthRecordRepository repository;
    @Mock
    FhirValidationService fhirValidationService;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldUseClientIdFromHeaderAndPersist() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HealthRecordConsumer consumer = new HealthRecordConsumer(ruleService, transformer, repository, objectMapper, kafkaTemplate, fhirValidationService, "atendimentos-brutos-dlq", true);

        String payload = "{\"paciente\":{\"cpf\":\"12345678900\"}}"; // no origem_id in payload
        ConsumerRecord<String, String> record = new ConsumerRecord<>("atendimentos-brutos", 0, 0L, "key", payload);
        record.headers().add(new RecordHeader("clientId", "UPA_RECIFE_01".getBytes(StandardCharsets.UTF_8)));

        UUID expected = UUID.nameUUIDFromBytes("UPA_RECIFE_01".getBytes());

        when(ruleService.getSpec(eq(expected))).thenReturn("[]");
        when(transformer.transform(anyString(), anyString())).thenReturn("{}");
        // validation no-op
        doNothing().when(fhirValidationService).validateOrThrow(anyString());

        consumer.consume(record);

        ArgumentCaptor<HealthRecord> cap = ArgumentCaptor.forClass(HealthRecord.class);
        verify(repository, times(1)).save(cap.capture());

        HealthRecord saved = cap.getValue();
        assertNotNull(saved.getClientId());
        assertEquals(expected, saved.getClientId());
        assertEquals("{}", saved.getResourceContent());
        assertEquals("12345678900", saved.getPatientId());
    }

    @Test
    void shouldFallbackToPayloadOrigemIdWhenHeaderMissing() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        HealthRecordConsumer consumer = new HealthRecordConsumer(ruleService, transformer, repository, objectMapper, kafkaTemplate, fhirValidationService, "atendimentos-brutos-dlq", true);

        String payload = "{\"origem_id\":\"UPA_LEGADO\",\"paciente\":{\"cpf\":\"99999999999\"}}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("atendimentos-brutos", 0, 0L, "key", payload);

        UUID expected = UUID.nameUUIDFromBytes("UPA_LEGADO".getBytes());

        when(ruleService.getSpec(eq(expected))).thenReturn("[]");
        when(transformer.transform(anyString(), anyString())).thenReturn("{}");
        doNothing().when(fhirValidationService).validateOrThrow(anyString());

        consumer.consume(record);

        ArgumentCaptor<HealthRecord> cap = ArgumentCaptor.forClass(HealthRecord.class);
        verify(repository, times(1)).save(cap.capture());

        HealthRecord saved = cap.getValue();
        assertEquals(expected, saved.getClientId());
        assertEquals("{}", saved.getResourceContent());
        assertEquals("99999999999", saved.getPatientId());
    }
}
