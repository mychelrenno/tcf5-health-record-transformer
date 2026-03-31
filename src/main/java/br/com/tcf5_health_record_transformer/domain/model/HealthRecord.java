package br.com.tcf5_health_record_transformer.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "health_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientId;
    private String resourceType;

    @Column(name = "client_id")
    private UUID clientId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_content", columnDefinition = "jsonb")
    private String resourceContent;

    private LocalDateTime processedAt;
}