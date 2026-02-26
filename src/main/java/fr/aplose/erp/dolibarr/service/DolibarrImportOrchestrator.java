package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportRun;
import fr.aplose.erp.dolibarr.repository.DolibarrImportRunRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DolibarrImportOrchestrator {

    private final DolibarrApiClient dolibarrApiClient;
    private final DolibarrImportLogService importLogService;
    private final DolibarrImportRunRepository runRepository;
    private final DolibarrDictionaryImporter dictionaryImporter;
    private final DolibarrThirdPartyImporter thirdPartyImporter;
    private final DolibarrContactImporter contactImporter;
    private final DolibarrProductImporter productImporter;
    private final DolibarrProposalImporter proposalImporter;
    private final DolibarrInvoiceImporter invoiceImporter;
    private final DolibarrPaymentImporter paymentImporter;
    private final DolibarrOrderImporter orderImporter;
    private final DolibarrStagingImporter stagingImporter;

    /**
     * Run full import for the given tenant. Caller must pass baseUrl and apiKey (from form or saved config).
     * TenantContext is set to tenantId for the duration of the import.
     */
    @Transactional
    public DolibarrImportRun runImport(String tenantId, String baseUrl, String apiKey, Long configId, Long createdBy) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String previousTenant = TenantContext.getCurrentTenantId();
        try {
            TenantContext.setCurrentTenantId(tenantId);
            dolibarrApiClient.configure(baseUrl, apiKey);

            DolibarrImportRun run = importLogService.createRun(tenantId, baseUrl, configId, createdBy);
            long runId = run.getId();

            importLogService.logInfo(runId, "START", "Import started from " + baseUrl);

            runStep(runId, "DICTIONARIES", () -> dictionaryImporter.importDictionaries(runId));
            runStep(runId, "THIRD_PARTIES", () -> thirdPartyImporter.importThirdParties(runId));
            runStep(runId, "CONTACTS", () -> contactImporter.importContacts(runId));
            runStep(runId, "PRODUCTS", () -> productImporter.importProducts(runId));
            runStep(runId, "PROPOSALS", () -> proposalImporter.importProposals(runId));
            runStep(runId, "INVOICES", () -> invoiceImporter.importInvoices(runId));
            runStep(runId, "PAYMENTS", () -> paymentImporter.importPayments(runId));
            runStep(runId, "ORDERS", () -> orderImporter.importOrders(runId));
            runStep(runId, "STAGING_TODO", () -> stagingImporter.importStaging(runId));

            String status = DolibarrImportRun.STATUS_SUCCESS;
            importLogService.finishRun(runId, status);
            importLogService.logInfo(runId, "END", "Import finished with status " + status);

            return runRepository.findById(runId).orElseThrow();
        } finally {
            if (previousTenant != null) {
                TenantContext.setCurrentTenantId(previousTenant);
            } else {
                TenantContext.clear();
            }
        }
    }

    private void runStep(Long runId, String stepName, Runnable step) {
        try {
            step.run();
        } catch (Exception e) {
            log.warn("Dolibarr import step {} failed: {}", stepName, e.getMessage());
            importLogService.logError(runId, stepName, null, e.getMessage(), null);
        }
    }
}
