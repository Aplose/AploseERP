package fr.aplose.erp.modules.commerce.repository;

import fr.aplose.erp.modules.commerce.entity.Proposal;
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
public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    Page<Proposal> findByTenantId(String tenantId, Pageable pageable);

    Page<Proposal> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    List<Proposal> findByTenantIdAndPipelineStageIdOrderByDateIssuedDesc(String tenantId, Long pipelineStageId);

    List<Proposal> findByTenantIdAndPipelineStageIdIsNullOrderByDateIssuedDesc(String tenantId);

    List<Proposal> findByTenantIdAndThirdPartyIdOrderByDateIssuedDesc(String tenantId, Long thirdPartyId);

    Optional<Proposal> findByIdAndTenantId(Long id, String tenantId);

    Optional<Proposal> findByReferenceAndTenantId(String reference, String tenantId);

    @Query("SELECT p FROM Proposal p WHERE p.tenantId = :tid " +
           "AND (LOWER(p.reference) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(p.title) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(p.thirdParty.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Proposal> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);

    long countByTenantIdAndStatusIn(String tenantId, java.util.Collection<String> statuses);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.reference, 5) AS int)), 0) FROM Proposal p WHERE p.tenantId = :tid AND p.reference LIKE 'PRO-%'")
    int findMaxReferenceNumber(@Param("tid") String tenantId);

    @Query("SELECT p FROM Proposal p WHERE p.tenantId = :tid AND p.status = 'SENT' " +
           "AND (p.dateValidUntil < :today OR (p.dateValidUntil IS NULL AND p.dateIssued < :limitDate)) " +
           "ORDER BY p.dateIssued ASC")
    List<Proposal> findProposalsToFollowUp(@Param("tid") String tenantId, @Param("today") LocalDate today, @Param("limitDate") LocalDate limitDate);
}
