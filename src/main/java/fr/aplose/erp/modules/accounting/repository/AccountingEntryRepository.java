package fr.aplose.erp.modules.accounting.repository;

import fr.aplose.erp.modules.accounting.entity.AccountingEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long> {

    @Query("SELECT DISTINCT e FROM AccountingEntry e LEFT JOIN FETCH e.lines l LEFT JOIN FETCH l.account WHERE e.id = :id AND e.tenantId = :tid")
    Optional<AccountingEntry> findByIdAndTenantIdWithLines(@Param("id") Long id, @Param("tid") String tenantId);

    Page<AccountingEntry> findByTenantIdOrderByEntryDateDesc(String tenantId, Pageable pageable);

    Page<AccountingEntry> findByTenantIdAndJournalIdOrderByEntryDateDesc(String tenantId, Long journalId, Pageable pageable);

    List<AccountingEntry> findByTenantIdAndEntryDateBetweenOrderByEntryDateAsc(String tenantId, LocalDate from, LocalDate to);

    Optional<AccountingEntry> findByIdAndTenantId(Long id, String tenantId);
}
