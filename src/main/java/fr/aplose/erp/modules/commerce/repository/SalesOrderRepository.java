package fr.aplose.erp.modules.commerce.repository;

import fr.aplose.erp.modules.commerce.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Page<SalesOrder> findByTenantId(String tenantId, Pageable pageable);

    Page<SalesOrder> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    Optional<SalesOrder> findByIdAndTenantId(Long id, String tenantId);

    @Query("SELECT o FROM SalesOrder o WHERE o.tenantId = :tid " +
           "AND (LOWER(o.reference) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(o.thirdParty.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<SalesOrder> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(o.reference, 4) AS int)), 0) FROM SalesOrder o WHERE o.tenantId = :tid AND o.reference LIKE 'SO-%'")
    int findMaxReferenceNumber(@Param("tid") String tenantId);
}
