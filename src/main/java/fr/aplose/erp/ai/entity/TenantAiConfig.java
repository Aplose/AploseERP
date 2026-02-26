package fr.aplose.erp.ai.entity;

import fr.aplose.erp.ai.config.EffectiveAiConfig;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_ai_config")
@Getter
@Setter
@NoArgsConstructor
public class TenantAiConfig {

    @Id
    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public EffectiveAiConfig toEffectiveConfig() {
        return new EffectiveAiConfig(baseUrl, apiKey, model != null && !model.isBlank() ? model : null);
    }
}
