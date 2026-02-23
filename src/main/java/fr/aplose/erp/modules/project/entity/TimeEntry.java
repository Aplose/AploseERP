package fr.aplose.erp.modules.project.entity;

import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_entries")
@Getter
@Setter
@NoArgsConstructor
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private ProjectTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "date_worked", nullable = false)
    private LocalDate dateWorked;

    @Column(name = "hours", precision = 8, scale = 2, nullable = false)
    private BigDecimal hours;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_billable", nullable = false)
    private boolean billable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
