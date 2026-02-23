package fr.aplose.erp.modules.leave.service;

import fr.aplose.erp.modules.leave.entity.LeaveType;
import fr.aplose.erp.modules.leave.repository.LeaveTypeRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    public List<LeaveType> findAllForCurrentTenant() {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return List.of();
        return leaveTypeRepository.findByTenantIdOrderBySortOrderAsc(tenantId);
    }

    public Optional<LeaveType> findByIdAndTenant(Long id) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return Optional.empty();
        return leaveTypeRepository.findByIdAndTenantId(id, tenantId);
    }
}
