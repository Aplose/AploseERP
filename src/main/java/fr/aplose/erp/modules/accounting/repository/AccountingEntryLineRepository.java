package fr.aplose.erp.modules.accounting.repository;

import fr.aplose.erp.modules.accounting.entity.AccountingEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountingEntryLineRepository extends JpaRepository<AccountingEntryLine, Long> {

    List<AccountingEntryLine> findByEntryIdOrderBySortOrderAscIdAsc(Long entryId);

    @Query("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM AccountingEntryLine l WHERE l.tenantId = :tid AND l.account.id = :accountId AND l.entry.entryDate BETWEEN :from AND :to")
    BigDecimal sumBalanceByTenantAndAccountAndDateRange(@Param("tid") String tenantId, @Param("accountId") Long accountId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
