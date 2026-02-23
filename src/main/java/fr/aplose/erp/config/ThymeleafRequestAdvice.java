package fr.aplose.erp.config;

import fr.aplose.erp.modules.nocode.dto.NoCodeMenuEntry;
import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.service.TenantModuleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds request URI, enabled modules, no-code menu entries and sidebar menu sections to the model for Thymeleaf templates.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class ThymeleafRequestAdvice {

    private final TenantModuleService tenantModuleService;
    private final MenuService menuService;

    @ModelAttribute
    public void addMenuModelAttributes(Model model, HttpServletRequest request) {
        String requestURI = request != null ? request.getRequestURI() : "";
        model.addAttribute("requestURI", requestURI);

        String tenantId = TenantContext.getCurrentTenantId();
        Set<String> enabledModules = Set.of();
        List<NoCodeMenuEntry> noCodeMenuEntries = List.of();
        if (tenantId != null && !tenantId.isBlank()) {
            enabledModules = tenantModuleService.getEnabledModuleCodes(tenantId);
            noCodeMenuEntries = tenantModuleService.getNoCodeMenuEntries(tenantId);
        }
        model.addAttribute("enabledModules", enabledModules);
        model.addAttribute("noCodeMenuEntries", noCodeMenuEntries);

        Set<String> userAuthorities = getCurrentUserAuthorities();
        model.addAttribute("sidebarMenuSections", menuService.buildSidebarSections(requestURI, enabledModules, noCodeMenuEntries, userAuthorities));
    }

    private Set<String> getCurrentUserAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null) {
            return Collections.emptySet();
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
    }
}
