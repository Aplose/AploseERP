package fr.aplose.erp.tenant.filter;

import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.repository.TenantRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Resolves the tenant of anonymous requests, before any request processing.
 * <p>
 * There is one access point for every company: no sub-domain per company, one login page.
 * A logged-in user's company is set by {@link AuthenticatedTenantFilter} (from the account),
 * and an API key carries its company. The only anonymous pages that need a company are the
 * public forms, reached at /t/{company code}/form/{form code}.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantResolutionFilter implements Filter {

    private static final String PATH_PREFIX  = "/t/";

    private final TenantRepository tenantRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            String tenantId = resolve(httpRequest);
            if (tenantId != null) {
                TenantContext.setCurrentTenantId(tenantId);
                log.debug("Tenant resolved: {}", tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolve(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri.startsWith(PATH_PREFIX)) {
            String[] parts = uri.substring(PATH_PREFIX.length()).split("/", 2);
            if (parts.length > 0 && !parts[0].isBlank()) {
                return tenantRepository.findByCodeAndActiveTrue(parts[0])
                        .map(t -> t.getId())
                        .orElse(null);
            }
        }
        return null;
    }
}
