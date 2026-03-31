package br.com.tcf5_health_record_transformer.domain.service;

import br.com.tcf5_health_record_transformer.infrastructure.repository.MappingRuleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RuleService {
    private final MappingRuleRepository repository;

    public RuleService(MappingRuleRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "joltRules", key = "#clientId.toString()")
    public String getSpec(UUID clientId) {
        return repository.findById(clientId)
                .map(br.com.tcf5_health_record_transformer.domain.model.MappingRule::getJoltSpec)
                .orElseThrow(() -> new RuntimeException("Regra não encontrada para: " + clientId));
    }
}