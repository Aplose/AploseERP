package fr.aplose.erp.modules.webhook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "webhook_endpoints")
@Getter
@Setter
@NoArgsConstructor
public class WebhookEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "secret", length = 255)
    private String secret;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "event_types", nullable = false, length = 500)
    private String eventTypes;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public List<String> getEventTypeList() {
        if (eventTypes == null || eventTypes.isBlank()) return List.of();
        return Arrays.stream(eventTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    public void setEventTypeList(List<String> events) {
        this.eventTypes = events != null ? String.join(",", events) : "";
    }
}
