package br.com.tcf5_health_record_transformer.infrastructure.repository;

import br.com.tcf5_health_record_transformer.domain.model.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

}
