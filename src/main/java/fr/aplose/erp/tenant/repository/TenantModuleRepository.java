package fr.aplose.erp.tenant.repository;

import fr.aplose.erp.tenant.entity.TenantModule;
import fr.aplose.erp.tenant.entity.TenantModuleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TenantModuleRepository extends JpaRepository<TenantModule, TenantModuleId> {

    List<TenantModule> findByTenantIdAndEnabledTrue(String tenantId);

    List<TenantModule> findByTenantIdOrderByModuleCode(String tenantId);

    Optional<TenantModule> findByTenantIdAndModuleCode(String tenantId, String moduleCode);

    boolean existsByTenantIdAndModuleCode(String tenantId, String moduleCode);

    @Query("SELECT DISTINCT tm FROM TenantModule tm JOIN FETCH tm.moduleDefinition md LEFT JOIN FETCH md.customEntityDefinitions WHERE tm.tenantId = :tenantId AND tm.moduleDefinition IS NOT NULL AND tm.enabled = true")
    List<TenantModule> findNoCodeEnabledByTenantId(String tenantId);

    Optional<TenantModule> findByTenantIdAndModuleDefinition_Id(String tenantId, Long moduleDefinitionId);
}
