package fr.aplose.erp.dolibarr.repository;

import fr.aplose.erp.dolibarr.entity.DolibarrImportStaging;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DolibarrImportStagingRepository extends JpaRepository<DolibarrImportStaging, Long> {

    List<DolibarrImportStaging> findByImportRunId(Long importRunId);
}
