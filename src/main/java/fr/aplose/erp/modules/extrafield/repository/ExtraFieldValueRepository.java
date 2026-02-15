package fr.aplose.erp.modules.extrafield.repository;

import fr.aplose.erp.modules.extrafield.entity.ExtraFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExtraFieldValueRepository extends JpaRepository<ExtraFieldValue, Long> {

    List<ExtraFieldValue> findByTenantIdAndEntityTypeAndEntityId(String tenantId, String entityType, Long entityId);

    Optional<ExtraFieldValue> findByTenantIdAndEntityTypeAndEntityIdAndFieldCode(
            String tenantId, String entityType, Long entityId, String fieldCode);

    void deleteByTenantIdAndEntityTypeAndEntityId(String tenantId, String entityType, Long entityId);
}
