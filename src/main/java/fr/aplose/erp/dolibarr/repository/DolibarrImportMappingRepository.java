package fr.aplose.erp.dolibarr.repository;

import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DolibarrImportMappingRepository extends JpaRepository<DolibarrImportMapping, Long> {

    Optional<DolibarrImportMapping> findByTenantIdAndImportRunIdAndDolibarrEntityAndDolibarrId(
            String tenantId, Long importRunId, String dolibarrEntity, Long dolibarrId);

    @Query("SELECT m.aploseId FROM DolibarrImportMapping m WHERE m.tenantId = :tid AND m.importRunId = :runId AND m.dolibarrEntity = :entity AND m.dolibarrId = :dolibarrId")
    Optional<Long> findAploseId(@Param("tid") String tenantId, @Param("runId") Long importRunId,
                                @Param("entity") String dolibarrEntity, @Param("dolibarrId") Long dolibarrId);

    List<DolibarrImportMapping> findByTenantIdAndImportRunId(String tenantId, Long importRunId);
}
