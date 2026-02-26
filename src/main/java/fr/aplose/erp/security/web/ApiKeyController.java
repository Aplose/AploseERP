package fr.aplose.erp.security.web;

import fr.aplose.erp.security.entity.ApiKey;
import fr.aplose.erp.security.service.ApiKeyService;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @PreAuthorize("hasAuthority('API_KEY_READ')")
    public String list(Model model) {
        List<ApiKey> keys = apiKeyService.findByTenant();
        model.addAttribute("apiKeys", keys);
        return "modules/admin/api-keys";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('API_KEY_CREATE')")
    public String create(@RequestParam String name,
                         @AuthenticationPrincipal ErpUserDetails user,
                         RedirectAttributes ra) {
        if (name == null || name.isBlank()) {
            ra.addFlashAttribute("error", "apikey.name.required");
            return "redirect:/admin/api-keys";
        }
        String rawKey = apiKeyService.create(user.getTenantId(), user.getUserId(), name.trim());
        ra.addFlashAttribute("message", "apikey.created");
        ra.addFlashAttribute("newApiKey", rawKey);
        ra.addFlashAttribute("newApiKeyName", name.trim());
        return "redirect:/admin/api-keys";
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('API_KEY_DELETE')")
    public String revoke(@PathVariable Long id, RedirectAttributes ra) {
        try {
            apiKeyService.revoke(id);
            ra.addFlashAttribute("message", "apikey.revoked");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "apikey.notfound");
        }
        return "redirect:/admin/api-keys";
    }
}
