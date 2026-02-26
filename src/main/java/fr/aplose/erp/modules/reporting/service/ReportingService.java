package fr.aplose.erp.modules.reporting.service;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.leave.entity.LeaveRequest;
import fr.aplose.erp.modules.leave.repository.LeaveRequestRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final InvoiceRepository invoiceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional(readOnly = true)
    public BigDecimal getSalesRevenue(LocalDate from, LocalDate to) {
        BigDecimal sum = invoiceRepository.sumTotalAmountByTenantAndTypeAndDateRange(
                TenantContext.getCurrentTenantId(), "SALES", from, to);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdue(TenantContext.getCurrentTenantId(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getLeaveRequestsSummary(String status, int limit) {
        var pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "dateStart"));
        if (status != null && !status.isBlank()) {
            return leaveRequestRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    TenantContext.getCurrentTenantId(), status, pageable).getContent();
        }
        return leaveRequestRepository.findByTenantIdOrderByCreatedAtDesc(
                TenantContext.getCurrentTenantId(), pageable).getContent();
    }
}
