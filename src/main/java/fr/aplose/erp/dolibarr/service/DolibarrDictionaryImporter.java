package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Imports Dolibarr dictionaries (countries, currencies, etc.) into AploseERP dictionary_items.
 * Dolibarr API may expose dictionnarycountries, dictionnarycurs, etc. - we try known resources.
 */
@Service
@RequiredArgsConstructor
public class DolibarrDictionaryImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final DictionaryService dictionaryService;

    /** Dolibarr API resource name -> AploseERP DictionaryType */
    private static final Map<String, String> RESOURCE_TO_TYPE = Map.ofEntries(
            Map.entry("dictionnarycountries", DictionaryType.COUNTRY),
            Map.entry("dictionnarycountry", DictionaryType.COUNTRY),
            Map.entry("currencies", DictionaryType.CURRENCY),
            Map.entry("dictionnarycurs", DictionaryType.CURRENCY),
            Map.entry("civilities", DictionaryType.CIVILITY),
            Map.entry("formejuridique", DictionaryType.LEGAL_FORM),
            Map.entry("paymentterms", DictionaryType.PAYMENT_METHOD),
            Map.entry("payment_vat", DictionaryType.PAYMENT_METHOD)
    );

    public void importDictionaries(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        for (Map.Entry<String, String> entry : RESOURCE_TO_TYPE.entrySet()) {
            String resource = entry.getKey();
            String dictType = entry.getValue();
            try {
                List<Map<String, Object>> list = client.getList(resource, Map.of("limit", "5000", "sortfield", "rowid"));
                if (list == null || list.isEmpty()) continue;

                int count = 0;
                for (Map<String, Object> item : list) {
                    try {
                        String code = toCode(item, resource);
                        String label = toLabel(item, resource);
                        if (code == null || code.isBlank()) continue;
                        dictionaryService.createOrUpdateForTenant(tenantId, dictType, code, label, count, true);
                        count++;
                    } catch (Exception e) {
                        logService.logWarn(runId, "DICTIONARIES", String.valueOf(DolibarrImportHelper.getDolibarrId(item)), e.getMessage());
                    }
                }
                logService.logInfo(runId, "DICTIONARIES", "Imported " + count + " items for " + dictType + " from " + resource);
            } catch (Exception e) {
                logService.logWarn(runId, "DICTIONARIES", resource, "Could not load " + resource + ": " + e.getMessage());
            }
        }
    }

    private static String toCode(Map<String, Object> m, String resource) {
        if (DictionaryType.COUNTRY.equals(RESOURCE_TO_TYPE.get(resource))) {
            String code = DolibarrImportHelper.getString(m, "code");
            if (code != null) return code;
            return DolibarrImportHelper.getString(m, "code_iso");
        }
        if (DictionaryType.CURRENCY.equals(RESOURCE_TO_TYPE.get(resource))) {
            return DolibarrImportHelper.getString(m, "code");
        }
        Long id = DolibarrImportHelper.getDolibarrId(m);
        String label = DolibarrImportHelper.getString(m, "label");
        if (label != null && !label.isBlank()) return label.substring(0, Math.min(50, label.length())).toUpperCase().replace(' ', '_');
        return id != null ? "D" + id : null;
    }

    private static String toLabel(Map<String, Object> m, String resource) {
        String label = DolibarrImportHelper.getString(m, "label");
        if (label != null) return label;
        label = DolibarrImportHelper.getString(m, "name");
        if (label != null) return label;
        return DolibarrImportHelper.getString(m, "code");
    }
}
