package fr.aplose.erp.modules.project.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id")
    private ThirdParty thirdParty;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "PLANNING";

    @Column(name = "priority", length = 20, nullable = false)
    private String priority = "MEDIUM";

    @Column(name = "date_start")
    private LocalDate dateStart;

    @Column(name = "date_end")
    private LocalDate dateEnd;

    @Column(name = "budget_amount", precision = 19, scale = 4)
    private BigDecimal budgetAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "billing_mode", length = 30)
    private String billingMode = "FIXED";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by")
    private Long createdById;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> memberList = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectTask> tasks = new ArrayList<>();

    public String getStatusBadgeClass() {
        return switch (status) {
            case "PLANNING" -> "bg-secondary";
            case "ACTIVE" -> "bg-primary";
            case "ON_HOLD" -> "bg-warning text-dark";
            case "COMPLETED" -> "bg-success";
            case "CANCELLED" -> "bg-dark";
            default -> "bg-secondary";
        };
    }
}
