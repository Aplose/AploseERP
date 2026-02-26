package fr.aplose.erp.modules.webhook.service;

import fr.aplose.erp.modules.webhook.entity.WebhookEndpoint;
import fr.aplose.erp.modules.webhook.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    private final WebhookEndpointRepository repository;
    private final WebhookSender sender;

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> findByTenant(String tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public WebhookEndpoint findByIdAndTenant(Long id, String tenantId) {
        return repository.findById(id)
                .filter(e -> tenantId.equals(e.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found: " + id));
    }

    @Transactional
    public WebhookEndpoint save(WebhookEndpoint endpoint, String tenantId) {
        endpoint.setTenantId(tenantId);
        if (endpoint.getId() == null) {
            endpoint.setCreatedAt(LocalDateTime.now());
        }
        return repository.save(endpoint);
    }

    @Transactional
    public void delete(Long id, String tenantId) {
        if (!repository.existsByIdAndTenantId(id, tenantId)) {
            throw new IllegalArgumentException("Webhook endpoint not found: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Dispatches event to all enabled endpoints for the tenant that subscribe to this event.
     * Delivery is asynchronous (fire-and-forget).
     */
    public void trigger(String tenantId, String eventType, Map<String, Object> data) {
        List<WebhookEndpoint> endpoints = repository.findByTenantIdAndEnabledTrue(tenantId);
        if (endpoints.isEmpty()) return;

        Map<String, Object> payload = Map.of(
                "event", eventType,
                "tenantId", tenantId,
                "entityType", eventType.contains(".") ? eventType.substring(0, eventType.indexOf('.')) : eventType,
                "timestamp", LocalDateTime.now().format(ISO),
                "data", data != null ? data : Map.of()
        );

        for (WebhookEndpoint ep : endpoints) {
            if (ep.getEventTypeList().stream().anyMatch(e -> "*".equals(e) || eventType.equals(e))) {
                sender.sendAsync(ep.getUrl(), ep.getSecret(), payload);
            }
        }
    }
}
