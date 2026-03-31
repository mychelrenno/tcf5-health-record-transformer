package br.com.tcf5_health_record_transformer.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class HealthRecordTest {

    @Test
    void builderShouldCreateInstanceAndAccessorsShouldWork() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 28, 12, 0);

        HealthRecord r1 = HealthRecord.builder()
                .id(1L)
                .patientId("PAT123")
                .resourceType("Observation")
                .resourceContent("{\"foo\":\"bar\"}")
                .processedAt(now)
                .build();

        HealthRecord r2 = HealthRecord.builder()
                .id(1L)
                .patientId("PAT123")
                .resourceType("Observation")
                .resourceContent("{\"foo\":\"bar\"}")
                .processedAt(now)
                .build();

        assertEquals(1L, r1.getId());
        assertEquals("PAT123", r1.getPatientId());
        assertEquals("Observation", r1.getResourceType());
        assertEquals("{\"foo\":\"bar\"}", r1.getResourceContent());
        assertEquals(now, r1.getProcessedAt());

        // equals/hashCode provided by Lombok @Data
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        // toString includes class name and some field
        String s = r1.toString();
        assertTrue(s.contains("PAT123"));
        assertTrue(s.contains("Observation"));
    }
}
