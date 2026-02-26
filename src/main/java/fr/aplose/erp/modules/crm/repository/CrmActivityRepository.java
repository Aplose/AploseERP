package fr.aplose.erp.modules.crm.repository;

import fr.aplose.erp.modules.crm.entity.CrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {

    List<CrmActivity> findByTenantIdOrderByDueDateAscCreatedAtDesc(String tenantId);

    List<CrmActivity> findByTenantIdAndThirdPartyIdOrderByDueDateAscCreatedAtDesc(String tenantId, Long thirdPartyId);

    List<CrmActivity> findByTenantIdAndCompletedAtIsNullOrderByDueDateAsc(String tenantId);
}
