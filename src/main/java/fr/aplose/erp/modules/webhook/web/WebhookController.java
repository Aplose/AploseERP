package fr.aplose.erp.modules.webhook.web;

import fr.aplose.erp.modules.webhook.entity.WebhookEndpoint;
import fr.aplose.erp.modules.webhook.service.WebhookService;
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
@RequestMapping("/admin/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @PreAuthorize("hasAuthority('WEBHOOK_READ')")
    public String list(Model model, @AuthenticationPrincipal ErpUserDetails user) {
        List<WebhookEndpoint> endpoints = webhookService.findByTenant(user.getTenantId());
        model.addAttribute("endpoints", endpoints);
        return "modules/admin/webhooks";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('WEBHOOK_CREATE')")
    public String create(@RequestParam String url,
                         @RequestParam(required = false) String secret,
                         @RequestParam(required = false) String description,
                         @RequestParam String eventTypes,
                         @AuthenticationPrincipal ErpUserDetails user,
                         RedirectAttributes ra) {
        String tenantId = user.getTenantId();
        if (url == null || url.isBlank()) {
            ra.addFlashAttribute("error", "webhook.url.required");
            return "redirect:/admin/webhooks";
        }
        if (eventTypes == null || eventTypes.isBlank()) {
            ra.addFlashAttribute("error", "webhook.events.required");
            return "redirect:/admin/webhooks";
        }
        WebhookEndpoint ep = new WebhookEndpoint();
        ep.setUrl(url.trim());
        ep.setSecret(secret != null && !secret.isBlank() ? secret.trim() : null);
        ep.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        ep.setEventTypes(eventTypes.trim());
        ep.setEnabled(true);
        webhookService.save(ep, tenantId);
        ra.addFlashAttribute("message", "webhook.created.msg");
        return "redirect:/admin/webhooks";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('WEBHOOK_UPDATE')")
    public String toggle(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails user, RedirectAttributes ra) {
        String tenantId = user.getTenantId();
        try {
            WebhookEndpoint ep = webhookService.findByIdAndTenant(id, tenantId);
            ep.setEnabled(!ep.isEnabled());
            webhookService.save(ep, tenantId);
            ra.addFlashAttribute("message", ep.isEnabled() ? "webhook.enabled.msg" : "webhook.disabled.msg");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "webhook.notfound");
        }
        return "redirect:/admin/webhooks";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('WEBHOOK_DELETE')")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails user, RedirectAttributes ra) {
        String tenantId = user.getTenantId();
        try {
            webhookService.delete(id, tenantId);
            ra.addFlashAttribute("message", "webhook.deleted.msg");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "webhook.notfound");
        }
        return "redirect:/admin/webhooks";
    }
}
