package fr.aplose.erp.mail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_templates", uniqueConstraints = @UniqueConstraint(columnNames = {"template_key", "locale"}))
@Getter
@Setter
@NoArgsConstructor
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", length = 50, nullable = false)
    private String templateKey;

    @Column(name = "subject", length = 500, nullable = false)
    private String subject;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "locale", length = 10, nullable = false)
    private String locale = "fr";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
