package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
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
import java.util.Objects;

/**
 * Imports customer invoice payments.
 * <p>
 * The Dolibarr REST API has no global payment list: payments are read per invoice with
 * GET invoices/{id}/payments, for every invoice already imported. Such a payment has no id in the
 * API answer, so it is identified by its Dolibarr reference (PAYxxxx) within its invoice.
 */
@Service
@RequiredArgsConstructor
public class DolibarrPaymentImporter {

    private static final String STEP = "PAYMENTS";

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final DolibarrImportMappingRepository mappingRepository;

    public void importPayments(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        int created = 0;
        int alreadyImported = 0;
        for (DolibarrImportMapping invoiceMapping : mappingRepository.findByTenantIdAndDolibarrEntity(tenantId, "invoices")) {
            Long doliInvoiceId = invoiceMapping.getDolibarrId();
            Invoice inv = invoiceRepository.findByIdAndTenantId(invoiceMapping.getAploseId(), tenantId).orElse(null);
            if (inv == null) continue;
            try {
                List<Map<String, Object>> payments = client.getList("invoices/" + doliInvoiceId + "/payments", Map.of());
                List<Payment> existing = paymentRepository.findByInvoiceIdOrderByPaymentDateAsc(inv.getId());
                for (Map<String, Object> m : payments) {
                    if ("discount".equals(DolibarrImportHelper.getString(m, "typeline"))) continue;
                    String ref = DolibarrImportHelper.getString(m, "ref");
                    if (ref != null && existing.stream().anyMatch(p -> Objects.equals(ref, p.getReference()))) {
                        alreadyImported++;
                        continue;
                    }
                    Payment pay = new Payment();
                    pay.setTenantId(tenantId);
                    pay.setInvoice(inv);
                    BigDecimal amount = DolibarrImportHelper.getBigDecimal(m, "amount");
                    pay.setAmount(amount != null ? amount : BigDecimal.ZERO);
                    pay.setCurrencyCode(inv.getCurrencyCode() != null ? inv.getCurrencyCode() : "EUR");
                    LocalDate date = DolibarrImportHelper.getLocalDate(m, "date");
                    pay.setPaymentDate(date != null ? date : LocalDate.now());
                    String type = DolibarrImportHelper.getString(m, "type");
                    pay.setPaymentMethod(type != null ? type : "BANK");
                    pay.setReference(ref);
                    String num = DolibarrImportHelper.getString(m, "num");
                    if (num != null && !num.isBlank()) pay.setNotes("N° " + num);
                    paymentRepository.save(pay);
                    created++;
                }
            } catch (Exception e) {
                logService.logError(runId, STEP, String.valueOf(doliInvoiceId), e.getMessage(), null);
            }
        }
        logService.logInfo(runId, STEP, created + " paiement(s) importé(s), " + alreadyImported + " déjà présent(s)");
    }
}
