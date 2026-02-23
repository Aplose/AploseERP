package fr.aplose.erp.modules.nocode.repository;

import fr.aplose.erp.modules.nocode.entity.CustomEntityData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomEntityDataRepository extends JpaRepository<CustomEntityData, Long> {

    List<CustomEntityData> findByTenantIdAndEntityDefinitionIdOrderByUpdatedAtDesc(String tenantId, Long entityDefinitionId, Pageable pageable);

    Page<CustomEntityData> findByTenantIdAndEntityDefinitionId(String tenantId, Long entityDefinitionId, Pageable pageable);

    long countByTenantIdAndEntityDefinitionId(String tenantId, Long entityDefinitionId);

    boolean existsByIdAndTenantId(Long id, String tenantId);
}
