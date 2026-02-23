package fr.aplose.erp.modules.nocode.web;

import fr.aplose.erp.modules.nocode.entity.ModuleDefinition;
import fr.aplose.erp.modules.nocode.service.ModuleDefinitionService;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/module-catalogue")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TENANT_READ') or hasAuthority('TENANT_UPDATE')")
public class ModuleCatalogueController {

    private final ModuleDefinitionService moduleDefinitionService;

    @GetMapping
    public String catalogue(Model model) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return "redirect:/dashboard";
        }
        List<ModuleDefinition> publicModules = moduleDefinitionService.findAllPublic();
        List<ModuleCatalogueEntry> entries = publicModules.stream()
                .map(md -> new ModuleCatalogueEntry(
                        md.getId(),
                        md.getCode(),
                        md.getName(),
                        md.getDescription(),
                        moduleDefinitionService.isActivatedForTenant(tenantId, md.getId())))
                .collect(Collectors.toList());
        model.addAttribute("entries", entries);
        return "modules/nocode/catalogue";
    }

    @PostMapping("/activate/{moduleDefinitionId}")
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String activate(@PathVariable Long moduleDefinitionId, RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return "redirect:/dashboard";
        }
        moduleDefinitionService.activateForTenant(tenantId, moduleDefinitionId);
        ra.addFlashAttribute("successMessage", "Module activé.");
        return "redirect:/admin/module-catalogue";
    }

    public record ModuleCatalogueEntry(Long id, String code, String name, String description, boolean activated) {}
}
