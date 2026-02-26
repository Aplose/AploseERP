package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.repository.DolibarrImportMappingRepository;
import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.entity.Payment;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.commerce.repository.PaymentRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DolibarrPaymentImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final DolibarrImportMappingRepository mappingRepository;

    public void importPayments(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        try {
            List<Map<String, Object>> list = client.getList("payments", Map.of("limit", "5000", "sortfield", "rowid"));
            if (list == null) return;

            for (Map<String, Object> m : list) {
                Long doliId = DolibarrImportHelper.getDolibarrId(m);
                if (doliId == null) continue;
                try {
                    Long fkFacture = DolibarrImportHelper.getLong(m, "fk_facture");
                    if (fkFacture == null) continue;
                    Optional<Long> invId = mappingRepository.findAploseId(tenantId, runId, "invoices", fkFacture);
                    if (invId.isEmpty()) continue;
                    Invoice inv = invoiceRepository.findByIdAndTenantId(invId.get(), tenantId).orElse(null);
                    if (inv == null) continue;

                    Payment pay = new Payment();
                    pay.setTenantId(tenantId);
                    pay.setInvoice(inv);
                    pay.setAmount(DolibarrImportHelper.getBigDecimal(m, "amount") != null ? DolibarrImportHelper.getBigDecimal(m, "amount") : BigDecimal.ZERO);
                    pay.setCurrencyCode(DolibarrImportHelper.getString(m, "currency_code") != null ? DolibarrImportHelper.getString(m, "currency_code") : "EUR");
                    pay.setPaymentDate(DolibarrImportHelper.getLocalDate(m, "datep") != null ? DolibarrImportHelper.getLocalDate(m, "datep") : LocalDate.now());
                    pay.setPaymentMethod(DolibarrImportHelper.getString(m, "payment_method") != null ? DolibarrImportHelper.getString(m, "payment_method") : "BANK");
                    pay.setReference(DolibarrImportHelper.getString(m, "num_payment"));
                    pay.setNotes(DolibarrImportHelper.getString(m, "note"));
                    paymentRepository.save(pay);
                } catch (Exception e) {
                    logService.logError(runId, "PAYMENTS", String.valueOf(doliId), e.getMessage(), null);
                }
            }
        } catch (Exception e) {
            logService.logWarn(runId, "PAYMENTS", null, "Payments import skipped: " + e.getMessage());
        }
    }
}
