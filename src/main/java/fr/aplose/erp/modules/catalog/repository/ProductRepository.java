package fr.aplose.erp.modules.catalog.repository;

import fr.aplose.erp.modules.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    Page<Product> findByTenantIdAndTypeAndDeletedAtIsNull(String tenantId, String type, Pageable pageable);

    Page<Product> findByTenantIdAndCategoryIdAndDeletedAtIsNull(String tenantId, Long categoryId, Pageable pageable);

    Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);

    Optional<Product> findByCodeAndTenantIdAndDeletedAtIsNull(String code, String tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tid AND p.deletedAt IS NULL " +
           "AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(p.code) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(p.barcode) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Product> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);

    long countByTenantIdAndDeletedAtIsNull(String tenantId);

    Page<Product> findByTenantIdAndSellableTrueAndActiveTrueAndDeletedAtIsNull(String tenantId, Pageable pageable);
}
