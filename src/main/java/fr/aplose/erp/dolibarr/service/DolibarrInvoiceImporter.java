package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
import fr.aplose.erp.dolibarr.repository.DolibarrImportMappingRepository;
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
    private final DolibarrImportMappingRepository mappingRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final ContactRepository contactRepository;

    public void importInvoices(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        List<Map<String, Object>> list = client.getList("invoices", Map.of("limit", "5000", "sortfield", "rowid"));
        if (list == null) return;

        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;
            try {
                Long socId = DolibarrImportHelper.getLong(m, "socid");
                if (socId == null) {
                    logService.logSkip(runId, "INVOICES", String.valueOf(doliId), "Missing socid");
                    continue;
                }
                Optional<Long> tpId = mappingRepository.findAploseId(tenantId, runId, "thirdparties", socId);
                if (tpId.isEmpty()) {
                    logService.logSkip(runId, "INVOICES", String.valueOf(doliId), "Third party not found: " + socId);
                    continue;
                }
                ThirdParty tp = thirdPartyRepository.findById(tpId.get()).filter(t -> tenantId.equals(t.getTenantId())).orElse(null);
                if (tp == null) continue;

                String ref = DolibarrImportHelper.getString(m, "ref");
                if (ref == null || ref.isBlank()) ref = "INV-" + doliId;
                if (invoiceRepository.findByReferenceAndTenantId(ref, tenantId).isPresent()) {
                    logService.logSkip(runId, "INVOICES", String.valueOf(doliId), "Reference exists: " + ref);
                    continue;
                }

                Invoice inv = new Invoice();
                inv.setReference(ref);
                inv.setType(DolibarrImportHelper.getInteger(m, "type") != null && DolibarrImportHelper.getInteger(m, "type") == 2 ? "PURCHASE" : "SALES");
                inv.setThirdParty(tp);
                Long contactId = DolibarrImportHelper.getLong(m, "fk_contact");
                if (contactId != null) {
                    mappingRepository.findAploseId(tenantId, runId, "contacts", contactId)
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
                BigDecimal paid = DolibarrImportHelper.getBigDecimal(m, "paye");
                inv.setAmountPaid(paid != null ? paid : BigDecimal.ZERO);
                inv.setAmountRemaining(inv.getTotalAmount().subtract(inv.getAmountPaid()));
                inv.setNotes(DolibarrImportHelper.getString(m, "note_private"));
                inv.setStatus(mapInvoiceStatus(DolibarrImportHelper.getInteger(m, "paye"), inv.getAmountRemaining().compareTo(BigDecimal.ZERO) > 0));

                Invoice saved = invoiceRepository.save(inv);
                DolibarrImportMapping mapping = new DolibarrImportMapping();
                mapping.setTenantId(tenantId);
                mapping.setImportRunId(runId);
                mapping.setDolibarrEntity("invoices");
                mapping.setDolibarrId(doliId);
                mapping.setAploseEntity("INVOICE");
                mapping.setAploseId(saved.getId());
                mappingRepository.save(mapping);
            } catch (Exception e) {
                logService.logError(runId, "INVOICES", String.valueOf(doliId), e.getMessage(), null);
            }
        }
    }

    private static String mapInvoiceStatus(Integer paye, boolean hasRemaining) {
        if (paye != null && paye == 1) return "PAID";
        if (hasRemaining) return "PARTIALLY_PAID";
        return "VALIDATED";
    }
}
