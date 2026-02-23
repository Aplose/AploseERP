package fr.aplose.erp.modules.leave.entity;

import fr.aplose.erp.tenant.context.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_types", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenantId();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
