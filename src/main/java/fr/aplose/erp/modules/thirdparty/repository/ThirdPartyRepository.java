package fr.aplose.erp.modules.thirdparty.repository;

import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThirdPartyRepository extends JpaRepository<ThirdParty, Long> {

    Page<ThirdParty> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    Page<ThirdParty> findByTenantIdAndCustomerTrueAndDeletedAtIsNull(String tenantId, Pageable pageable);

    Page<ThirdParty> findByTenantIdAndSupplierTrueAndDeletedAtIsNull(String tenantId, Pageable pageable);

    Optional<ThirdParty> findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);

    Optional<ThirdParty> findByCodeAndTenantIdAndDeletedAtIsNull(String code, String tenantId);

    @Query("SELECT t FROM ThirdParty t WHERE t.tenantId = :tid AND t.deletedAt IS NULL " +
           "AND (LOWER(t.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(t.code) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(t.email) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(t.phone) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<ThirdParty> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);
}
