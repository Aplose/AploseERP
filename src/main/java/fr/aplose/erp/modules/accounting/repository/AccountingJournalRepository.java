package fr.aplose.erp.modules.accounting.repository;

import fr.aplose.erp.modules.accounting.entity.AccountingJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingJournalRepository extends JpaRepository<AccountingJournal, Long> {

    List<AccountingJournal> findByTenantIdAndActiveTrueOrderByCodeAsc(String tenantId);

    List<AccountingJournal> findByTenantIdOrderByCodeAsc(String tenantId);

    Optional<AccountingJournal> findByIdAndTenantId(Long id, String tenantId);
}
