package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
import fr.aplose.erp.dolibarr.repository.DolibarrImportMappingRepository;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DolibarrThirdPartyImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final ThirdPartyRepository thirdPartyRepository;
    private final DolibarrImportMappingRepository mappingRepository;

    public void importThirdParties(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        List<Map<String, Object>> list = client.getList("thirdparties", Map.of("limit", "10000", "sortfield", "t.rowid"));
        if (list == null) return;

        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;

            try {
                String code = DolibarrImportHelper.thirdPartyCode(m);
                String name = DolibarrImportHelper.getString(m, "name");
                if (name == null) name = DolibarrImportHelper.getString(m, "nom");
                if (name == null || name.isBlank()) {
                    logService.logSkip(runId, "THIRD_PARTIES", String.valueOf(doliId), "Missing name");
                    continue;
                }

                if (thirdPartyRepository.findByCodeAndTenantIdAndDeletedAtIsNull(code, tenantId).isPresent()) {
                    logService.logSkip(runId, "THIRD_PARTIES", String.valueOf(doliId), "Code already exists: " + code);
                    continue;
                }

                ThirdParty tp = new ThirdParty();
                tp.setCode(code);
                tp.setName(name);
                tp.setCustomer(DolibarrImportHelper.getBoolean(m, "client"));
                tp.setSupplier(DolibarrImportHelper.getBoolean(m, "fournisseur"));
                tp.setProspect(DolibarrImportHelper.getBoolean(m, "prospect"));
                if (tp.isCustomer() && tp.isSupplier()) tp.setType("BOTH");
                else if (tp.isCustomer()) tp.setType("CUSTOMER");
                else if (tp.isSupplier()) tp.setType("SUPPLIER");
                else if (tp.isProspect()) tp.setType("PROSPECT");
                else tp.setType("OTHER");

                tp.setLegalForm(DolibarrImportHelper.getString(m, "forme_juridique"));
                tp.setTaxId(DolibarrImportHelper.getString(m, "tva_intra"));
                tp.setRegistrationNo(DolibarrImportHelper.getString(m, "siren"));
                tp.setWebsite(DolibarrImportHelper.getString(m, "url"));
                tp.setPhone(DolibarrImportHelper.getString(m, "phone"));
                tp.setFax(DolibarrImportHelper.getString(m, "fax"));
                tp.setEmail(DolibarrImportHelper.getString(m, "email"));
                tp.setAddressLine1(DolibarrImportHelper.getString(m, "address"));
                tp.setAddressLine2(DolibarrImportHelper.getString(m, "address2"));
                tp.setCity(DolibarrImportHelper.getString(m, "town"));
                tp.setStateProvince(DolibarrImportHelper.getString(m, "state"));
                tp.setPostalCode(DolibarrImportHelper.getString(m, "zip"));
                tp.setCountryCode(DolibarrImportHelper.getString(m, "country_code"));
                if (tp.getCountryCode() == null) tp.setCountryCode(DolibarrImportHelper.getString(m, "code_pays"));
                tp.setCurrencyCode(DolibarrImportHelper.getString(m, "code_devise"));
                Integer payTerm = DolibarrImportHelper.getInteger(m, "payment_terms");
                tp.setPaymentTerms(payTerm != null ? payTerm.shortValue() : null);
                BigDecimal cred = DolibarrImportHelper.getBigDecimal(m, "credit_limit");
                tp.setCreditLimit(cred);
                tp.setNotes(DolibarrImportHelper.getString(m, "note_private"));
                tp.setStatus("ACTIVE");

                ThirdParty saved = thirdPartyRepository.save(tp);

                DolibarrImportMapping mapping = new DolibarrImportMapping();
                mapping.setTenantId(tenantId);
                mapping.setImportRunId(runId);
                mapping.setDolibarrEntity("thirdparties");
                mapping.setDolibarrId(doliId);
                mapping.setAploseEntity("THIRD_PARTY");
                mapping.setAploseId(saved.getId());
                mappingRepository.save(mapping);
            } catch (Exception e) {
                logService.logError(runId, "THIRD_PARTIES", String.valueOf(doliId), e.getMessage(), null);
            }
        }
    }
}
