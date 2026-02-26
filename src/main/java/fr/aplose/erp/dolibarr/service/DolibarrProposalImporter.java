package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
import fr.aplose.erp.dolibarr.repository.DolibarrImportMappingRepository;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
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
public class DolibarrProposalImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final ProposalRepository proposalRepository;
    private final DolibarrImportMappingRepository mappingRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final ContactRepository contactRepository;

    public void importProposals(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        for (String resource : List.of("propals", "proposals")) {
            List<Map<String, Object>> list = client.getList(resource, Map.of("limit", "5000", "sortfield", "rowid"));
            if (list == null || list.isEmpty()) continue;

            for (Map<String, Object> m : list) {
                Long doliId = DolibarrImportHelper.getDolibarrId(m);
                if (doliId == null) continue;
                try {
                    Long socId = DolibarrImportHelper.getLong(m, "socid");
                    if (socId == null) {
                        logService.logSkip(runId, "PROPOSALS", String.valueOf(doliId), "Missing socid");
                        continue;
                    }
                    Optional<Long> tpId = mappingRepository.findAploseId(tenantId, runId, "thirdparties", socId);
                    if (tpId.isEmpty()) {
                        logService.logSkip(runId, "PROPOSALS", String.valueOf(doliId), "Third party not found: " + socId);
                        continue;
                    }
                    ThirdParty tp = thirdPartyRepository.findById(tpId.get()).filter(t -> tenantId.equals(t.getTenantId())).orElse(null);
                    if (tp == null) continue;

                    String ref = DolibarrImportHelper.getString(m, "ref");
                    if (ref == null || ref.isBlank()) ref = "PRO-" + doliId;
                    if (proposalRepository.findByReferenceAndTenantId(ref, tenantId).isPresent()) {
                        logService.logSkip(runId, "PROPOSALS", String.valueOf(doliId), "Reference exists: " + ref);
                        continue;
                    }

                    Proposal p = new Proposal();
                    p.setReference(ref);
                    p.setThirdParty(tp);
                    Long contactId = DolibarrImportHelper.getLong(m, "fk_user_creat");
                    if (contactId == null) contactId = DolibarrImportHelper.getLong(m, "fk_contact");
                    if (contactId != null) {
                        mappingRepository.findAploseId(tenantId, runId, "contacts", contactId)
                                .flatMap(contactRepository::findById)
                                .filter(c -> tenantId.equals(c.getTenantId()))
                                .ifPresent(p::setContact);
                    }
                    p.setTitle(DolibarrImportHelper.getString(m, "title"));
                    p.setDateIssued(DolibarrImportHelper.getLocalDate(m, "date_creation") != null ? DolibarrImportHelper.getLocalDate(m, "date_creation") : LocalDate.now());
                    p.setDateValidUntil(DolibarrImportHelper.getLocalDate(m, "date_fin_validite"));
                    p.setCurrencyCode(DolibarrImportHelper.getString(m, "currency_code") != null ? DolibarrImportHelper.getString(m, "currency_code") : "EUR");
                    p.setSubtotal(DolibarrImportHelper.getBigDecimal(m, "total_ht") != null ? DolibarrImportHelper.getBigDecimal(m, "total_ht") : BigDecimal.ZERO);
                    p.setDiscountAmount(DolibarrImportHelper.getBigDecimal(m, "remise") != null ? DolibarrImportHelper.getBigDecimal(m, "remise") : BigDecimal.ZERO);
                    p.setVatAmount(DolibarrImportHelper.getBigDecimal(m, "total_tva") != null ? DolibarrImportHelper.getBigDecimal(m, "total_tva") : BigDecimal.ZERO);
                    p.setTotalAmount(DolibarrImportHelper.getBigDecimal(m, "total_ttc") != null ? DolibarrImportHelper.getBigDecimal(m, "total_ttc") : p.getSubtotal().add(p.getVatAmount()));
                    p.setNotes(DolibarrImportHelper.getString(m, "note_private"));
                    p.setStatus(mapProposalStatus(DolibarrImportHelper.getInteger(m, "statut")));

                    Proposal saved = proposalRepository.save(p);
                    DolibarrImportMapping mapping = new DolibarrImportMapping();
                    mapping.setTenantId(tenantId);
                    mapping.setImportRunId(runId);
                    mapping.setDolibarrEntity(resource);
                    mapping.setDolibarrId(doliId);
                    mapping.setAploseEntity("PROPOSAL");
                    mapping.setAploseId(saved.getId());
                    mappingRepository.save(mapping);
                } catch (Exception e) {
                    logService.logError(runId, "PROPOSALS", String.valueOf(doliId), e.getMessage(), null);
                }
            }
            break;
        }
    }

    private static String mapProposalStatus(Integer statut) {
        if (statut == null) return "DRAFT";
        return switch (statut) {
            case 0 -> "DRAFT";
            case 1 -> "SENT";
            case 2 -> "ACCEPTED";
            case 3 -> "REFUSED";
            case 4 -> "CANCELLED";
            case 5 -> "CONVERTED";
            default -> "DRAFT";
        };
    }
}
