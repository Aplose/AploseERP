package fr.aplose.erp.modules.ged.repository;

import fr.aplose.erp.modules.ged.entity.GedDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GedDocumentRepository extends JpaRepository<GedDocument, Long> {

    List<GedDocument> findByTenantIdAndEntityTypeAndEntityIdOrderByVersionDesc(String tenantId, String entityType, Long entityId);

    Page<GedDocument> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    @Query("SELECT d FROM GedDocument d WHERE d.tenantId = :tid AND (LOWER(d.fileName) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<GedDocument> searchByFileName(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);

    Optional<GedDocument> findByIdAndTenantId(Long id, String tenantId);
}
