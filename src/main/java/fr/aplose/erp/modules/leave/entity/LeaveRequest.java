package fr.aplose.erp.modules.leave.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "date_start", nullable = false)
    private LocalDate dateStart;

    @Column(name = "date_end", nullable = false)
    private LocalDate dateEnd;

    @Column(name = "half_day_start", nullable = false)
    private boolean halfDayStart = false;

    @Column(name = "half_day_end", nullable = false)
    private boolean halfDayEnd = false;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "DRAFT";

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validator_id")
    private User validator;

    @Column(name = "approved_by_id")
    private Long approvedById;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "denied_by_id")
    private Long deniedById;

    @Column(name = "denied_at")
    private LocalDateTime deniedAt;

    @Column(name = "response_comment", columnDefinition = "TEXT")
    private String responseComment;

    public String getStatusBadgeClass() {
        return switch (status) {
            case "DRAFT" -> "bg-secondary";
            case "PENDING_APPROVAL" -> "bg-primary";
            case "APPROVED" -> "bg-success";
            case "DENIED" -> "bg-danger";
            case "CANCELLED" -> "bg-dark";
            default -> "bg-secondary";
        };
    }
}
