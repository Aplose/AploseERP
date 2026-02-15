package fr.aplose.erp.modules.extrafield.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "extrafield_values")
@Getter
@Setter
@NoArgsConstructor
public class ExtraFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "field_code", length = 50, nullable = false)
    private String fieldCode;

    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;
}
