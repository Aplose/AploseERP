package fr.aplose.erp.modules.project.entity;

import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ProjectTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private ProjectTask parentTask;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProjectTask> subTasks = new ArrayList<>();

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "TODO";

    @Column(name = "priority", length = 20, nullable = false)
    private String priority = "MEDIUM";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "date_start")
    private LocalDate dateStart;

    @Column(name = "date_due")
    private LocalDate dateDue;

    @Column(name = "date_completed")
    private LocalDate dateCompleted;

    @Column(name = "estimated_hours", precision = 8, scale = 2)
    private BigDecimal estimatedHours;

    @Column(name = "logged_hours", precision = 8, scale = 2, nullable = false)
    private BigDecimal loggedHours = BigDecimal.ZERO;

    @Column(name = "progress_pct", nullable = false)
    private short progressPct = 0;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
