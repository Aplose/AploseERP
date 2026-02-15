package fr.aplose.erp.modules.fieldconfig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "field_visibility_config")
@Getter
@Setter
@NoArgsConstructor
public class FieldVisibilityConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "field_name", length = 100, nullable = false)
    private String fieldName;

    @Column(name = "visible_on_list", nullable = false)
    private boolean visibleOnList = true;

    @Column(name = "visible_on_detail", nullable = false)
    private boolean visibleOnDetail = true;

    @Column(name = "visible_on_form", nullable = false)
    private boolean visibleOnForm = true;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;
}
