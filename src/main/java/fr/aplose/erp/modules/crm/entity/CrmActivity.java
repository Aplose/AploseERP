package fr.aplose.erp.modules.crm.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.modules.agenda.entity.AgendaEvent;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "crm_activities")
@Getter
@Setter
@NoArgsConstructor
public class CrmActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @NotBlank
    @Column(name = "activity_type", length = 30, nullable = false)
    private String activityType;

    @NotBlank
    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "due_time")
    private LocalTime dueTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agenda_event_id")
    private AgendaEvent agendaEvent;

    @Column(name = "created_by")
    private Long createdById;

    public boolean isOverdue() {
        if (completedAt != null) return false;
        if (dueDate == null) return false;
        return dueDate.isBefore(LocalDate.now());
    }
}
