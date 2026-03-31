package br.com.tcf5_health_record_transformer.domain.service;

import br.com.tcf5_health_record_transformer.domain.model.MappingRule;
import br.com.tcf5_health_record_transformer.infrastructure.repository.MappingRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class RuleServiceTest {

    @Test
    @DisplayName("getSpec should return the jolt spec when mapping rule exists")
    void getSpecReturnsSpecWhenExists() {
        MappingRuleRepository repo = Mockito.mock(MappingRuleRepository.class);
        UUID clientId = UUID.nameUUIDFromBytes("CLIENT_1".getBytes());
        MappingRule rule = new MappingRule(clientId, "[{}]");

        when(repo.findById(eq(clientId))).thenReturn(Optional.of(rule));

        RuleService service = new RuleService(repo);

        String spec = service.getSpec(clientId);

        assertEquals("[{}]", spec);
    }

    @Test
    @DisplayName("getSpec should throw when mapping rule is not found")
    void getSpecThrowsWhenNotFound() {
        MappingRuleRepository repo = Mockito.mock(MappingRuleRepository.class);
        UUID clientId = UUID.nameUUIDFromBytes("MISSING".getBytes());

        when(repo.findById(eq(clientId))).thenReturn(Optional.empty());

        RuleService service = new RuleService(repo);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getSpec(clientId));

        assertEquals("Regra não encontrada para: " + clientId, ex.getMessage());
    }
}
