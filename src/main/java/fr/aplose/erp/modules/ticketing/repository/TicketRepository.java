package fr.aplose.erp.modules.ticketing.repository;

import fr.aplose.erp.modules.ticketing.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<Ticket> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status, Pageable pageable);

    Page<Ticket> findByTenantIdAndAssigneeIdOrderByCreatedAtDesc(String tenantId, Long assigneeId, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.tenantId = :tid AND (LOWER(t.subject) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Ticket> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);

    Optional<Ticket> findByIdAndTenantId(Long id, String tenantId);

    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN FETCH t.comments WHERE t.id = :id AND t.tenantId = :tid")
    Optional<Ticket> findByIdAndTenantIdWithComments(@Param("id") Long id, @Param("tid") String tenantId);
}
