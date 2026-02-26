package fr.aplose.erp.modules.publicform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Public form definition (contact, quote request, etc.) per tenant.
 * Rendered at /form/{code} or /t/{tenantCode}/form/{code}.
 */
@Entity
@Table(name = "public_forms")
@Getter
@Setter
@NoArgsConstructor
public class PublicForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "fields_json", nullable = false, columnDefinition = "TEXT")
    private String fieldsJson;

    @Column(name = "success_message", length = 500)
    private String successMessage;

    @Column(name = "notify_emails", length = 500)
    private String notifyEmails;

    @Column(name = "captcha_enabled", nullable = false)
    private boolean captchaEnabled = true;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
