package br.com.tcf5_health_record_transformer.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "mapping_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingRule {
    @Id
    private UUID clientId; // Ex: UUID

    @Lob
    @Column(columnDefinition = "text")
    private String joltSpec;
}