package fr.aplose.erp.modules.leave.repository;

import fr.aplose.erp.modules.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    List<LeaveType> findByTenantIdOrderBySortOrderAsc(String tenantId);

    Optional<LeaveType> findByTenantIdAndCode(String tenantId, String code);

    Optional<LeaveType> findByIdAndTenantId(Long id, String tenantId);
}
