package fr.aplose.erp.modules.leave.repository;

import fr.aplose.erp.modules.leave.entity.LeaveRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Optional<LeaveRequest> findByIdAndTenantId(Long id, String tenantId);

    Page<LeaveRequest> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<LeaveRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status, Pageable pageable);

    Page<LeaveRequest> findByTenantIdAndRequesterIdOrderByCreatedAtDesc(String tenantId, Long requesterId, Pageable pageable);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.tenantId = :tid " +
           "AND (:status IS NULL OR lr.status = :status) " +
           "AND (:requesterId IS NULL OR lr.requester.id = :requesterId) " +
           "AND (:fromDate IS NULL OR lr.dateEnd >= :fromDate) " +
           "AND (:toDate IS NULL OR lr.dateStart <= :toDate) " +
           "ORDER BY lr.createdAt DESC")
    Page<LeaveRequest> findByTenantIdAndFilters(@Param("tid") String tenantId,
                                                @Param("status") String status,
                                                @Param("requesterId") Long requesterId,
                                                @Param("fromDate") LocalDate fromDate,
                                                @Param("toDate") LocalDate toDate,
                                                Pageable pageable);
}
