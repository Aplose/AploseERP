package fr.aplose.erp.dolibarr.repository;

import fr.aplose.erp.dolibarr.entity.DolibarrImportRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DolibarrImportRunRepository extends JpaRepository<DolibarrImportRun, Long> {

    List<DolibarrImportRun> findByTenantIdOrderByStartedAtDesc(String tenantId, Pageable pageable);
}
