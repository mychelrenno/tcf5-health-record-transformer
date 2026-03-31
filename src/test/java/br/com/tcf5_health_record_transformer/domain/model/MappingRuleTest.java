package br.com.tcf5_health_record_transformer.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MappingRuleTest {

    @Test
    void gettersAndEqualsShouldWork() {
        UUID id = UUID.nameUUIDFromBytes("CLIENT_A".getBytes());
        MappingRule m1 = new MappingRule(id, "[{}]");
        MappingRule m2 = new MappingRule(id, "[{}]");

        assertEquals(id, m1.getClientId());
        assertEquals("[{}]", m1.getJoltSpec());

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
