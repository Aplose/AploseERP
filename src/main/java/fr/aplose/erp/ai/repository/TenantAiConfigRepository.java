package fr.aplose.erp.ai.repository;

import fr.aplose.erp.ai.entity.TenantAiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantAiConfigRepository extends JpaRepository<TenantAiConfig, String> {

    Optional<TenantAiConfig> findByTenantId(String tenantId);
}
