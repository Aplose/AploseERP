package fr.aplose.erp.dolibarr.repository;

import fr.aplose.erp.dolibarr.entity.DolibarrImportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DolibarrImportLogRepository extends JpaRepository<DolibarrImportLog, Long> {

    List<DolibarrImportLog> findByImportRunIdOrderByIdAsc(Long importRunId);

    Page<DolibarrImportLog> findByImportRunId(Long importRunId, Pageable pageable);

    Page<DolibarrImportLog> findByImportRunIdAndLevel(Long importRunId, String level, Pageable pageable);
}
