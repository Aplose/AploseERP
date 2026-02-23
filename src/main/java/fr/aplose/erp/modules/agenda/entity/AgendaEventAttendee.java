package fr.aplose.erp.modules.agenda.entity;

import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agenda_event_attendees")
@Getter
@Setter
@NoArgsConstructor
public class AgendaEventAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private AgendaEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "INVITED";

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
}
