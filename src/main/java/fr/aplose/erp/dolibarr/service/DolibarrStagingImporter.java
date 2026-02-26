package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.aplose.erp.dolibarr.entity.DolibarrImportStaging;
import fr.aplose.erp.dolibarr.repository.DolibarrImportStagingRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Imports Dolibarr entities that have no AploseERP equivalent yet.
 * Data is stored in dolibarr_import_staging and logged as TODO.
 */
@Service
@RequiredArgsConstructor
public class DolibarrStagingImporter {

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final DolibarrImportStagingRepository stagingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Resources to fetch and store as staging (TODO until module exists). */
    private static final List<String> STAGING_RESOURCES = List.of(
            "supplier_orders", "supplier_proposals", "contracts", "projects", "tasks",
            "stock_movements", "expensereports", "trips", "holiday"
    );

    public void importStaging(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        for (String resource : STAGING_RESOURCES) {
            try {
                List<Map<String, Object>> list = client.getList(resource, Map.of("limit", "5000", "sortfield", "rowid"));
                if (list == null || list.isEmpty()) continue;

                for (Map<String, Object> item : list) {
                    Long extId = DolibarrImportHelper.getDolibarrId(item);
                    if (extId == null) continue;
                    try {
                        String json = objectMapper.writeValueAsString(item);
                        DolibarrImportStaging staging = new DolibarrImportStaging();
                        staging.setImportRunId(runId);
                        staging.setEntityDolibarr(resource);
                        staging.setExternalId(extId);
                        staging.setPayloadJson(json);
                        stagingRepository.save(staging);
                        logService.logInfo(runId, "STAGING_TODO", "Donnée importée en attente de module (TODO): " + resource + " id=" + extId);
                    } catch (JsonProcessingException e) {
                        logService.logWarn(runId, "STAGING_TODO", String.valueOf(extId), "Failed to serialize: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                logService.logWarn(runId, "STAGING_TODO", resource, "Could not load " + resource + ": " + e.getMessage());
            }
        }
    }
}
