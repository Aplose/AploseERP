package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
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
public class DolibarrInvoiceImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final InvoiceRepository invoiceRepository;
    private final DolibarrMappingService mappingService;
    private final ThirdPartyRepository thirdPartyRepository;
    private final ContactRepository contactRepository;

    public void importInvoices(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        List<Map<String, Object>> list = client.getAll("invoices", Map.of("sortfield", "t.rowid"));
        if (list == null) return;

        int alreadyImported = 0;
        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;
            if (mappingService.isImported(tenantId, "invoices", doliId)) {
                alreadyImported++;
                continue;
            }
            try {
                Long socId = DolibarrImportHelper.getLong(m, "socid");
                if (socId == null) {
                    logService.logSkip(runId, "INVOICES", String.valueOf(doliId), "Missing socid");
                    continue;
                }
                Optional<Long> tpId = mappingService.find(tenantId, "thirdparties", socId);
                if (tpId.isEmpty()) {
                    logService.logSkip(runId, "INVOICES", String.valueOf(doliId), "Third party not found: " + socId);
                    continue;
                }
                ThirdParty tp = thirdPartyRepository.findById(tpId.get()).filter(t -> tenantId.equals(t.getTenantId())).orElse(null);
                if (tp == null) continue;

                String ref = DolibarrImportHelper.getString(m, "ref");
                if (ref == null || ref.isBlank()) ref = "INV-" + doliId;
                var existing = invoiceRepository.findByReferenceAndTenantId(ref, tenantId);
                if (existing.isPresent()) {
                    mappingService.record(tenantId, runId, "invoices", doliId, "INVOICE", existing.get().getId());
                    logService.logInfo(runId, "INVOICES", "Dolibarr id " + doliId + " rattaché à l'existant (réf. " + ref + ")");
                    continue;
                }

                Invoice inv = new Invoice();
                inv.setReference(ref);
                // The "invoices" API only lists customer invoices. Dolibarr type 2 is a credit note (negative amounts),
                // not a supplier invoice: it stays a sales document so that sales reports net it out.
                inv.setType("SALES");
                inv.setThirdParty(tp);
                Long contactId = DolibarrImportHelper.getLong(m, "fk_contact");
                if (contactId != null) {
                    mappingService.find(tenantId, "contacts", contactId)
                            .flatMap(contactRepository::findById)
                            .filter(c -> tenantId.equals(c.getTenantId()))
                            .ifPresent(inv::setContact);
                }
                inv.setDateIssued(DolibarrImportHelper.getLocalDate(m, "date") != null ? DolibarrImportHelper.getLocalDate(m, "date") : LocalDate.now());
                inv.setDateDue(DolibarrImportHelper.getLocalDate(m, "date_lim_reglement") != null ? DolibarrImportHelper.getLocalDate(m, "date_lim_reglement") : inv.getDateIssued().plusDays(30));
                inv.setCurrencyCode(DolibarrImportHelper.getString(m, "currency_code") != null ? DolibarrImportHelper.getString(m, "currency_code") : "EUR");
                BigDecimal total = DolibarrImportHelper.getBigDecimal(m, "total_ttc");
                inv.setTotalAmount(total != null ? total : BigDecimal.ZERO);
                inv.setSubtotal(DolibarrImportHelper.getBigDecimal(m, "total_ht") != null ? DolibarrImportHelper.getBigDecimal(m, "total_ht") : BigDecimal.ZERO);
                inv.setVatAmount(DolibarrImportHelper.getBigDecimal(m, "total_tva") != null ? DolibarrImportHelper.getBigDecimal(m, "total_tva") : BigDecimal.ZERO);
                inv.setDiscountAmount(DolibarrImportHelper.getBigDecimal(m, "remise") != null ? DolibarrImportHelper.getBigDecimal(m, "remise") : BigDecimal.ZERO);
                // "paye" is only a 0/1 flag; the amount already paid is "totalpaid" (API since Dolibarr 10)
                BigDecimal paid = DolibarrImportHelper.getBigDecimal(m, "totalpaid");
                inv.setAmountPaid(paid != null ? paid : BigDecimal.ZERO);
                BigDecimal remaining = DolibarrImportHelper.getBigDecimal(m, "remaintopay");
                inv.setAmountRemaining(remaining != null ? remaining : inv.getTotalAmount().subtract(inv.getAmountPaid()));
                inv.setNotes(DolibarrImportHelper.getString(m, "note_private"));
                inv.setStatus(mapInvoiceStatus(DolibarrImportHelper.getStatus(m), DolibarrImportHelper.getInteger(m, "paye"),
                        inv.getAmountPaid(), inv.getAmountRemaining()));

                Invoice saved = invoiceRepository.save(inv);
                mappingService.record(tenantId, runId, "invoices", doliId, "INVOICE", saved.getId());
            } catch (Exception e) {
                logService.logError(runId, "INVOICES", String.valueOf(doliId), e.getMessage(), null);
            }
        }
        logService.logInfo(runId, "INVOICES", alreadyImported + " facture(s) déjà importé(s) lors d'un import précédent");
    }

    /** Dolibarr invoice status: 0 draft, 1 validated (unpaid), 2 closed (paid or partly paid), 3 abandoned. */
    static String mapInvoiceStatus(Integer status, Integer paye, BigDecimal paid, BigDecimal remaining) {
        if (status != null && status == 0) return "DRAFT";
        if (status != null && status == 3) return "CANCELLED";
        if ((paye != null && paye == 1) || remaining.signum() <= 0) return "PAID";
        if (paid.signum() > 0) return "PARTIALLY_PAID";
        return "VALIDATED";
    }
}
