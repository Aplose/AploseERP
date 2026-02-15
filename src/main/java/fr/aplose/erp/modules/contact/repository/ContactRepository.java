package fr.aplose.erp.modules.contact.repository;

import fr.aplose.erp.modules.contact.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Page<Contact> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    List<Contact> findByThirdPartyIdAndDeletedAtIsNull(Long thirdPartyId);

    Optional<Contact> findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tid AND c.deletedAt IS NULL " +
           "AND (LOWER(c.firstName) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(c.phone) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Contact> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);
}
