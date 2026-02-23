package fr.aplose.erp.modules.nocode.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "custom_entity_definitions")
@Getter
@Setter
@NoArgsConstructor
public class CustomEntityDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_definition_id", nullable = false)
    private ModuleDefinition moduleDefinition;

    @Column(name = "code", length = 80, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "fields_schema", columnDefinition = "TEXT")
    private String fieldsSchema;

    @Column(name = "list_columns", columnDefinition = "TEXT")
    private String listColumns;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
