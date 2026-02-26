package fr.aplose.erp.ai.web;

import fr.aplose.erp.ai.entity.TenantAiConfig;
import fr.aplose.erp.ai.service.AiService;
import fr.aplose.erp.security.service.ErpUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/ai")
public class AiAdminController {

    private final AiService aiService;

    public AiAdminController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AI_USE')")
    public String status(Model model, @AuthenticationPrincipal ErpUserDetails user) {
        String tenantId = user.getTenantId();
        model.addAttribute("available", aiService.isAvailable());
        Optional<TenantAiConfig> tenantConfig = aiService.getTenantConfig(tenantId);
        model.addAttribute("tenantConfig", tenantConfig.orElse(null));
        return "modules/admin/ai-status";
    }

    @PostMapping("/tenant-config")
    @PreAuthorize("hasAuthority('AI_USE')")
    public String saveTenantConfig(@AuthenticationPrincipal ErpUserDetails user,
                                   @RequestParam String baseUrl,
                                   @RequestParam(required = false) String apiKey,
                                   @RequestParam(required = false) String model,
                                   RedirectAttributes ra) {
        if (baseUrl == null || baseUrl.isBlank()) {
            ra.addFlashAttribute("error", "ai.admin.baseUrlRequired");
            return "redirect:/admin/ai";
        }
        aiService.saveTenantConfig(user.getTenantId(), baseUrl, apiKey, model);
        ra.addFlashAttribute("message", "ai.admin.configSaved");
        return "redirect:/admin/ai";
    }
}
