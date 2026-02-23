package fr.aplose.erp.modules.agenda.entity;

import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.project.entity.Project;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agenda_events")
@Getter
@Setter
@NoArgsConstructor
public class AgendaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", length = 30, nullable = false)
    private String type = "MEETING";

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PLANNED";

    @Column(name = "all_day", nullable = false)
    private boolean allDay = false;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @Column(name = "location", length = 500)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id")
    private ThirdParty thirdParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "recurrence_rule", length = 500)
    private String recurrenceRule;

    @Column(name = "recurrence_end")
    private LocalDate recurrenceEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_event_id")
    private AgendaEvent parentEvent;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "is_private", nullable = false)
    private boolean privacy = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgendaEventAttendee> attendees = new ArrayList<>();

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
