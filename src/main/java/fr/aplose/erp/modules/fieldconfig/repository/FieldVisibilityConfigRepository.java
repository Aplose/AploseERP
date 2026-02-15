package fr.aplose.erp.modules.fieldconfig.repository;

import fr.aplose.erp.modules.fieldconfig.entity.FieldVisibilityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldVisibilityConfigRepository extends JpaRepository<FieldVisibilityConfig, Long> {

    List<FieldVisibilityConfig> findByTenantIdAndEntityType(String tenantId, String entityType);

    Optional<FieldVisibilityConfig> findByTenantIdAndEntityTypeAndFieldName(
            String tenantId, String entityType, String fieldName);
}
