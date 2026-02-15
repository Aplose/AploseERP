package fr.aplose.erp.modules.catalog.repository;

import fr.aplose.erp.modules.catalog.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByTenantIdOrderBySortOrderAscNameAsc(String tenantId);

    Optional<ProductCategory> findByIdAndTenantId(Long id, String tenantId);

    Optional<ProductCategory> findByCodeAndTenantId(String code, String tenantId);
}
