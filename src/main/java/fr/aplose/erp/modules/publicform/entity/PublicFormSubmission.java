package fr.aplose.erp.modules.publicform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "public_form_submissions")
@Getter
@Setter
@NoArgsConstructor
public class PublicFormSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private PublicForm form;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "data_json", nullable = false, columnDefinition = "TEXT")
    private String dataJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
}
