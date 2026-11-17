package fr.aplose.erp.tenant.filter;

import fr.aplose.erp.security.service.ErpUserDetails;
import fr.aplose.erp.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Once a user is authenticated, the current tenant is the user's tenant, and nothing else.
 * <p>
 * {@link TenantResolutionFilter} guesses the tenant from the request (sub-domain, /t/ prefix,
 * X-Tenant-ID header...), which is needed before login. Without this filter, a user logged in to
 * company A could read company B's data by sending the same session cookie with B's host name or
 * with an X-Tenant-ID header naming B. Registered in the security chain, after authentication.
 * Deliberately not a Spring bean, so that it is not also registered as a plain servlet filter.
 */
@Slf4j
public class AuthenticatedTenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ErpUserDetails user) {
            String resolved = TenantContext.getCurrentTenantId();
            if (!Objects.equals(resolved, user.getTenantId())) {
                if (resolved != null) {
                    log.warn("User {} of tenant {} sent a request resolved to tenant {}: using the user's tenant",
                            user.getUsername(), user.getTenantId(), resolved);
                }
                TenantContext.setCurrentTenantId(user.getTenantId());
            }
        }
        filterChain.doFilter(request, response);
    }
}
