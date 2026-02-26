package fr.aplose.erp.modules.treasury.service;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreasuryService {

    private final InvoiceRepository invoiceRepository;

    /** Forecast entries for the next N months: inflows (sales to collect), outflows (purchases to pay), balance. */
    @Transactional(readOnly = true)
    public List<TreasuryForecastEntry> getForecastByMonth(int numberOfMonths) {
        String tid = TenantContext.getCurrentTenantId();
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusMonths(numberOfMonths);
        List<TreasuryForecastEntry> result = new ArrayList<>();
        YearMonth ym = YearMonth.from(from);
        YearMonth endYm = YearMonth.from(to);
        while (!ym.isAfter(endYm)) {
            LocalDate periodStart = ym.atDay(1);
            LocalDate periodEnd = ym.atEndOfMonth();
            BigDecimal inflows = sumRemaining(invoiceRepository.findForTreasuryByTypeAndDueBetween(tid, "SALES", periodStart, periodEnd));
            BigDecimal outflows = sumRemaining(invoiceRepository.findForTreasuryByTypeAndDueBetween(tid, "PURCHASE", periodStart, periodEnd));
            result.add(new TreasuryForecastEntry(ym.toString(), periodStart, periodEnd, inflows, outflows, inflows.subtract(outflows)));
            ym = ym.plusMonths(1);
        }
        return result;
    }

    /** Inflows: sales invoices to collect (due in range). */
    @Transactional(readOnly = true)
    public List<Invoice> getExpectedInflows(LocalDate from, LocalDate to) {
        return invoiceRepository.findForTreasuryByTypeAndDueBetween(
                TenantContext.getCurrentTenantId(), "SALES", from, to);
    }

    /** Outflows: purchase invoices to pay (due in range). */
    @Transactional(readOnly = true)
    public List<Invoice> getExpectedOutflows(LocalDate from, LocalDate to) {
        return invoiceRepository.findForTreasuryByTypeAndDueBetween(
                TenantContext.getCurrentTenantId(), "PURCHASE", from, to);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpectedInflows(LocalDate from, LocalDate to) {
        return sumRemaining(getExpectedInflows(from, to));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExpectedOutflows(LocalDate from, LocalDate to) {
        return sumRemaining(getExpectedOutflows(from, to));
    }

    private static BigDecimal sumRemaining(List<Invoice> list) {
        return list.stream()
                .map(Invoice::getAmountRemaining)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record TreasuryForecastEntry(String periodLabel, LocalDate periodStart, LocalDate periodEnd,
                                       BigDecimal inflows, BigDecimal outflows, BigDecimal balance) {}
}
