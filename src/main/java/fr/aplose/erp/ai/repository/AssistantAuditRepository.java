package fr.aplose.erp.ai.repository;

import fr.aplose.erp.ai.entity.AssistantAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssistantAuditRepository extends JpaRepository<AssistantAudit, Long> {

    Page<AssistantAudit> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
