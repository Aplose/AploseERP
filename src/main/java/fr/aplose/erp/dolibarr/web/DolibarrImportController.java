package fr.aplose.erp.dolibarr.web;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportLog;
import fr.aplose.erp.dolibarr.entity.DolibarrImportRun;
import fr.aplose.erp.dolibarr.repository.DolibarrImportLogRepository;
import fr.aplose.erp.dolibarr.repository.DolibarrImportRunRepository;
import fr.aplose.erp.dolibarr.service.DolibarrImportOrchestrator;
import fr.aplose.erp.security.service.ErpUserDetails;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/dolibarr-import")
@PreAuthorize("hasAuthority('DOLIBARR_IMPORT')")
@RequiredArgsConstructor
public class DolibarrImportController {

    private final DolibarrApiClient dolibarrApiClient;
    private final DolibarrImportOrchestrator orchestrator;
    private final DolibarrImportRunRepository runRepository;
    private final DolibarrImportLogRepository logRepository;

    @GetMapping
    public String index(Model model) {
        String tenantId = TenantContext.getCurrentTenantId();
        List<DolibarrImportRun> runs = runRepository.findByTenantIdOrderByStartedAtDesc(tenantId, PageRequest.of(0, 10));
        model.addAttribute("runs", runs);
        model.addAttribute("lastRun", runs.isEmpty() ? null : runs.get(0));
        model.addAttribute("pageTitle", "Import Dolibarr");
        return "modules/admin/dolibarr-import";
    }

    @PostMapping("/test")
    public String testConnection(@RequestParam String baseUrl, @RequestParam String apiKey, RedirectAttributes ra) {
        dolibarrApiClient.configure(baseUrl, apiKey);
        boolean ok = dolibarrApiClient.testConnection();
        if (ok) {
            ra.addFlashAttribute("successMessage", "Connexion réussie.");
        } else {
            ra.addFlashAttribute("errorMessage", "Échec de la connexion. Vérifiez l'URL et la clé API.");
        }
        return "redirect:/admin/dolibarr-import";
    }

    @PostMapping("/run")
    public String runImport(
            @RequestParam String baseUrl,
            @RequestParam String apiKey,
            @AuthenticationPrincipal ErpUserDetails user,
            RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            ra.addFlashAttribute("errorMessage", "Tenant non défini.");
            return "redirect:/admin/dolibarr-import";
        }
        try {
            Long userId = user != null && user.getUserId() != null ? user.getUserId() : null;
            DolibarrImportRun run = orchestrator.runImport(tenantId, baseUrl, apiKey, null, userId);
            ra.addFlashAttribute("successMessage", "Import terminé. Statut: " + run.getStatus());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Erreur lors de l'import: " + e.getMessage());
        }
        return "redirect:/admin/dolibarr-import";
    }

    @GetMapping("/runs/{runId}/logs")
    public String logs(@PathVariable Long runId, Model model) {
        String tenantId = TenantContext.getCurrentTenantId();
        DolibarrImportRun run = runRepository.findById(runId)
                .filter(r -> tenantId.equals(r.getTenantId()))
                .orElse(null);
        if (run == null) {
            return "redirect:/admin/dolibarr-import";
        }
        List<DolibarrImportLog> logs = logRepository.findByImportRunIdOrderByIdAsc(runId);
        model.addAttribute("run", run);
        model.addAttribute("logs", logs);
        model.addAttribute("pageTitle", "Import Dolibarr - Logs");
        return "modules/admin/dolibarr-import-logs";
    }
}
