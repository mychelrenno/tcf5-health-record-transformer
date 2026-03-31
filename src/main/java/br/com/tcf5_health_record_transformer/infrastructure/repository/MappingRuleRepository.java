package br.com.tcf5_health_record_transformer.infrastructure.repository;

import br.com.tcf5_health_record_transformer.domain.model.MappingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MappingRuleRepository extends JpaRepository<MappingRule, UUID> {
}
