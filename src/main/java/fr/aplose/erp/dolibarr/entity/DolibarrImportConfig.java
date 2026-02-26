package fr.aplose.erp.dolibarr.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dolibarr_import_config")
@Getter
@Setter
@NoArgsConstructor
public class DolibarrImportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "base_url", length = 500, nullable = false)
    private String baseUrl;

    @Column(name = "api_key_encrypted", length = 500)
    private String apiKeyEncrypted;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
