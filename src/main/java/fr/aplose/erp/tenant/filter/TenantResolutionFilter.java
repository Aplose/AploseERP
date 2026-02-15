package fr.aplose.erp.tenant.filter;

import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.repository.TenantRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Resolves the current tenant before any request processing.
 *
 * Resolution order:
 *  1. Subdomain: acme.erp.aplose.fr -> code "acme"
 *  2. URL path prefix: /t/acme/... -> code "acme"
 *  3. Session attribute TENANT_ID (dev / localhost)
 *  4. HTTP header X-Tenant-ID (future API clients)
 *  5. Fallback: first active tenant in DB (single-tenant dev mode)
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TenantResolutionFilter implements Filter {

    private static final String SESSION_ATTR = "TENANT_ID";
    private static final String HEADER_NAME  = "X-Tenant-ID";
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
        // 1. Subdomain
        String host = req.getServerName();
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            if (!"www".equals(subdomain) && !"localhost".equals(subdomain)) {
                return tenantRepository.findByCodeAndActiveTrue(subdomain)
                        .map(t -> t.getId())
                        .orElse(null);
            }
        }

        // 2. URL path prefix /t/{code}/
        String uri = req.getRequestURI();
        if (uri.startsWith(PATH_PREFIX)) {
            String[] parts = uri.substring(PATH_PREFIX.length()).split("/", 2);
            if (parts.length > 0 && !parts[0].isBlank()) {
                return tenantRepository.findByCodeAndActiveTrue(parts[0])
                        .map(t -> t.getId())
                        .orElse(null);
            }
        }

        // 3. Session attribute
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object attrVal = session.getAttribute(SESSION_ATTR);
            if (attrVal instanceof String tenantId && !tenantId.isBlank()) {
                return tenantId;
            }
        }

        // 4. HTTP header
        String header = req.getHeader(HEADER_NAME);
        if (header != null && !header.isBlank()) {
            return header;
        }

        // 5. Fallback: single-tenant dev mode
        return tenantRepository.findAll().stream()
                .filter(t -> t.isActive())
                .findFirst()
                .map(t -> t.getId())
                .orElse(null);
    }
}
