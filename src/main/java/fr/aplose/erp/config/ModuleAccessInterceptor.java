package fr.aplose.erp.config;

import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.service.TenantModuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Interceptor that blocks access to module URLs when the module is disabled for the current tenant.
 */
@Component
@RequiredArgsConstructor
public class ModuleAccessInterceptor implements HandlerInterceptor {

    private static final Map<String, String> PATH_PREFIX_TO_MODULE = Map.ofEntries(
            Map.entry("/third-parties", "CRM_THIRD_PARTY"),
            Map.entry("/contacts", "CRM_CONTACT"),
            Map.entry("/crm/activities", "CRM_ACTIVITY"),
            Map.entry("/proposals", "COMMERCE_PROPOSAL"),
            Map.entry("/pipeline", "PIPELINE"),
            Map.entry("/orders", "COMMERCE_ORDER"),
            Map.entry("/invoices", "COMMERCE_INVOICE"),
            Map.entry("/follow-up", "FOLLOW_UP"),
            Map.entry("/contracts", "BUSINESS_CONTRACT"),
            Map.entry("/products", "CATALOG_PRODUCT"),
            Map.entry("/projects", "PROJECT"),
            Map.entry("/agenda", "AGENDA"),
            Map.entry("/bank", "BANK"),
            Map.entry("/treasury", "TREASURY"),
            Map.entry("/accounting", "ACCOUNTING"),
            Map.entry("/hr", "HR"),
            Map.entry("/leave-requests", "LEAVE_REQUEST"),
            Map.entry("/ged", "GED"),
            Map.entry("/ticketing", "TICKETING"),
            Map.entry("/reporting", "REPORTING"),
            Map.entry("/automation", "AUTOMATION")
    );

    private final TenantModuleService tenantModuleService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return true;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (!path.startsWith("/")) path = "/" + path;

        for (Map.Entry<String, String> e : PATH_PREFIX_TO_MODULE.entrySet()) {
            if (path.equals(e.getKey()) || path.startsWith(e.getKey() + "/")) {
                if (!tenantModuleService.isModuleEnabled(tenantId, e.getValue())) {
                    response.sendRedirect(request.getContextPath() + "/dashboard?moduleDisabled=1");
                    return false;
                }
                break;
            }
        }
        return true;
    }
}
