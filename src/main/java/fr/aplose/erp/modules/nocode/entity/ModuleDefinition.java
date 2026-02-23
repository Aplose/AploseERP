package fr.aplose.erp.modules.nocode.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "module_definitions")
@Getter
@Setter
@NoArgsConstructor
public class ModuleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 80, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "version", length = 50, nullable = false)
    private String version = "1.0.0";

    @Column(name = "author_tenant_id", length = 36)
    private String authorTenantId;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "schema_json", columnDefinition = "TEXT")
    private String schemaJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "moduleDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomEntityDefinition> customEntityDefinitions = new ArrayList<>();

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
