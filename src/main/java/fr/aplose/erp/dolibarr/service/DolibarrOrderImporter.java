package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
import fr.aplose.erp.dolibarr.repository.DolibarrImportMappingRepository;
import fr.aplose.erp.modules.commerce.entity.SalesOrder;
import fr.aplose.erp.modules.commerce.repository.SalesOrderRepository;
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
public class DolibarrOrderImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final SalesOrderRepository orderRepository;
    private final DolibarrImportMappingRepository mappingRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final ContactRepository contactRepository;

    public void importOrders(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        List<Map<String, Object>> list = client.getList("orders", Map.of("limit", "5000", "sortfield", "rowid"));
        if (list == null) return;

        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;
            try {
                Long socId = DolibarrImportHelper.getLong(m, "socid");
                if (socId == null) {
                    logService.logSkip(runId, "ORDERS", String.valueOf(doliId), "Missing socid");
                    continue;
                }
                Optional<Long> tpId = mappingRepository.findAploseId(tenantId, runId, "thirdparties", socId);
                if (tpId.isEmpty()) {
                    logService.logSkip(runId, "ORDERS", String.valueOf(doliId), "Third party not found: " + socId);
                    continue;
                }
                ThirdParty tp = thirdPartyRepository.findById(tpId.get()).filter(t -> tenantId.equals(t.getTenantId())).orElse(null);
                if (tp == null) continue;

                String ref = DolibarrImportHelper.getString(m, "ref");
                if (ref == null || ref.isBlank()) ref = "SO-" + doliId;
                if (orderRepository.findByReferenceAndTenantId(ref, tenantId).isPresent()) {
                    logService.logSkip(runId, "ORDERS", String.valueOf(doliId), "Reference exists: " + ref);
                    continue;
                }

                SalesOrder order = new SalesOrder();
                order.setReference(ref);
                order.setThirdParty(tp);
                Long contactId = DolibarrImportHelper.getLong(m, "fk_contact");
                if (contactId != null) {
                    mappingRepository.findAploseId(tenantId, runId, "contacts", contactId)
                            .flatMap(contactRepository::findById)
                            .filter(c -> tenantId.equals(c.getTenantId()))
                            .ifPresent(order::setContact);
                }
                order.setDateOrdered(DolibarrImportHelper.getLocalDate(m, "date_commande") != null ? DolibarrImportHelper.getLocalDate(m, "date_commande") : LocalDate.now());
                order.setDateExpected(DolibarrImportHelper.getLocalDate(m, "date_livraison"));
                order.setCurrencyCode(DolibarrImportHelper.getString(m, "currency_code") != null ? DolibarrImportHelper.getString(m, "currency_code") : "EUR");
                order.setSubtotal(DolibarrImportHelper.getBigDecimal(m, "total_ht") != null ? DolibarrImportHelper.getBigDecimal(m, "total_ht") : BigDecimal.ZERO);
                order.setDiscountAmount(DolibarrImportHelper.getBigDecimal(m, "remise") != null ? DolibarrImportHelper.getBigDecimal(m, "remise") : BigDecimal.ZERO);
                order.setVatAmount(DolibarrImportHelper.getBigDecimal(m, "total_tva") != null ? DolibarrImportHelper.getBigDecimal(m, "total_tva") : BigDecimal.ZERO);
                order.setTotalAmount(DolibarrImportHelper.getBigDecimal(m, "total_ttc") != null ? DolibarrImportHelper.getBigDecimal(m, "total_ttc") : order.getSubtotal().add(order.getVatAmount()));
                order.setNotes(DolibarrImportHelper.getString(m, "note_private"));
                order.setStatus(mapOrderStatus(DolibarrImportHelper.getInteger(m, "statut")));

                SalesOrder saved = orderRepository.save(order);
                DolibarrImportMapping mapping = new DolibarrImportMapping();
                mapping.setTenantId(tenantId);
                mapping.setImportRunId(runId);
                mapping.setDolibarrEntity("orders");
                mapping.setDolibarrId(doliId);
                mapping.setAploseEntity("SALES_ORDER");
                mapping.setAploseId(saved.getId());
                mappingRepository.save(mapping);
            } catch (Exception e) {
                logService.logError(runId, "ORDERS", String.valueOf(doliId), e.getMessage(), null);
            }
        }
    }

    private static String mapOrderStatus(Integer statut) {
        if (statut == null) return "CONFIRMED";
        return switch (statut) {
            case 0 -> "DRAFT";
            case 1 -> "CONFIRMED";
            case 2 -> "PROCESSING";
            case 3 -> "DELIVERED";
            case 4 -> "CANCELLED";
            default -> "CONFIRMED";
        };
    }
}
