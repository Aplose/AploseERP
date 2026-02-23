package fr.aplose.erp.tenant.entity;

import fr.aplose.erp.modules.nocode.entity.ModuleDefinition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_modules")
@IdClass(fr.aplose.erp.tenant.entity.TenantModuleId.class)
@Getter
@Setter
@NoArgsConstructor
public class TenantModule {

    @Id
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private String tenantId;

    @Id
    @Column(name = "module_code", length = 80, nullable = false, updatable = false)
    private String moduleCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_definition_id")
    private ModuleDefinition moduleDefinition;

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
