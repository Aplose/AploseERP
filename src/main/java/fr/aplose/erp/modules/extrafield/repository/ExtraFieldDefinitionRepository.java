package fr.aplose.erp.modules.extrafield.repository;

import fr.aplose.erp.modules.extrafield.entity.ExtraFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExtraFieldDefinitionRepository extends JpaRepository<ExtraFieldDefinition, Long> {

    List<ExtraFieldDefinition> findByTenantIdAndEntityTypeOrderBySortOrderAsc(String tenantId, String entityType);

    List<ExtraFieldDefinition> findByTenantIdAndEntityTypeAndActiveTrueOrderBySortOrderAsc(String tenantId, String entityType);

    Optional<ExtraFieldDefinition> findByIdAndTenantId(Long id, String tenantId);

    Optional<ExtraFieldDefinition> findByTenantIdAndEntityTypeAndFieldCode(String tenantId, String entityType, String fieldCode);
}
