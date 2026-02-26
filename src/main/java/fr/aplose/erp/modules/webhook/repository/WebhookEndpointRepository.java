package fr.aplose.erp.modules.webhook.repository;

import fr.aplose.erp.modules.webhook.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, Long> {

    List<WebhookEndpoint> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<WebhookEndpoint> findByTenantIdAndEnabledTrue(String tenantId);

    boolean existsByIdAndTenantId(Long id, String tenantId);
}
