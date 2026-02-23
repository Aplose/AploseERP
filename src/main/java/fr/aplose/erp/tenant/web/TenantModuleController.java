package fr.aplose.erp.tenant.web;

import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.module.CoreModule;
import fr.aplose.erp.tenant.service.TenantModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/modules")
@RequiredArgsConstructor
public class TenantModuleController {

    private final TenantModuleService tenantModuleService;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_READ')")
    public String list(Model model) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return "redirect:/dashboard";
        }
        Set<String> enabledCodes = tenantModuleService.getEnabledModuleCodes(tenantId);
        List<CoreModule> modules = tenantModuleService.getAllCoreModules();
        model.addAttribute("modules", modules);
        model.addAttribute("enabledCodes", enabledCodes);
        return "modules/admin/modules";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String update(@RequestParam(value = "enabled", required = false) List<String> enabledCodes,
                        RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return "redirect:/dashboard";
        }
        Set<String> enabled = enabledCodes != null ? Set.copyOf(enabledCodes) : Set.of();
        for (CoreModule core : CoreModule.values()) {
            tenantModuleService.updateModuleEnabled(tenantId, core.getCode(), enabled.contains(core.getCode()));
        }
        ra.addFlashAttribute("successMessage", "Modules updated.");
        return "redirect:/admin/modules";
    }
}
