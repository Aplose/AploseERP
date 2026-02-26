package fr.aplose.erp.modules.bank.repository;

import fr.aplose.erp.modules.bank.entity.BankMovement;
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
public interface BankMovementRepository extends JpaRepository<BankMovement, Long> {

    Page<BankMovement> findByTenantIdOrderByMovementDateDesc(String tenantId, Pageable pageable);

    Page<BankMovement> findByTenantIdAndAccountIdOrderByMovementDateDesc(String tenantId, Long accountId, Pageable pageable);

    List<BankMovement> findByTenantIdAndAccountIdAndMovementDateBetweenOrderByMovementDateAsc(
        String tenantId, Long accountId, LocalDate from, LocalDate to);

    Optional<BankMovement> findByIdAndTenantId(Long id, String tenantId);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM BankMovement m WHERE m.tenantId = :tid AND m.account.id = :accountId")
    java.math.BigDecimal sumAmountByTenantIdAndAccountId(@Param("tid") String tenantId, @Param("accountId") Long accountId);
}

