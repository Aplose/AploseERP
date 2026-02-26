package fr.aplose.erp.modules.commerce.repository;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByTenantId(String tenantId, Pageable pageable);

    Page<Invoice> findByTenantIdAndType(String tenantId, String type, Pageable pageable);

    Page<Invoice> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    Optional<Invoice> findByIdAndTenantId(Long id, String tenantId);

    List<Invoice> findByTenantIdAndThirdPartyIdOrderByDateIssuedDesc(String tenantId, Long thirdPartyId);

    Optional<Invoice> findByReferenceAndTenantId(String reference, String tenantId);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tid " +
           "AND (LOWER(i.reference) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(i.thirdParty.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Invoice> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);

    long countByTenantIdAndStatusIn(String tenantId, Collection<String> statuses);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tid AND i.dateDue < :today AND i.amountRemaining > 0 AND i.status NOT IN ('PAID','CANCELLED') ORDER BY i.dateDue ASC")
    List<Invoice> findOverdue(@Param("tid") String tenantId, @Param("today") LocalDate today);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.reference, 5) AS int)), 0) FROM Invoice i WHERE i.tenantId = :tid AND i.reference LIKE :prefix")
    int findMaxReferenceNumber(@Param("tid") String tenantId, @Param("prefix") String prefix);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.tenantId = :tid AND i.type = :type AND i.status IN ('VALIDATED','PARTIALLY_PAID','PAID') AND i.dateIssued BETWEEN :from AND :to")
    BigDecimal sumTotalAmountByTenantAndTypeAndDateRange(@Param("tid") String tenantId, @Param("type") String type, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tid AND i.type = :type AND i.amountRemaining > 0 AND i.status IN ('VALIDATED', 'PARTIALLY_PAID') AND i.dateDue BETWEEN :from AND :to ORDER BY i.dateDue ASC")
    List<Invoice> findForTreasuryByTypeAndDueBetween(@Param("tid") String tenantId, @Param("type") String type, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
