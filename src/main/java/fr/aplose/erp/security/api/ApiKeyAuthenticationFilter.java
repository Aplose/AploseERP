package fr.aplose.erp.security.api;

import fr.aplose.erp.security.service.ApiKeyService;
import fr.aplose.erp.security.service.ErpUserDetails;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates requests that carry an API key (X-API-Key or Authorization: Bearer).
 * Sets tenant context and a full UserDetails-based authentication so @PreAuthorize works.
 */
@Slf4j
@Component
@Order(-100)
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_AUTH = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (request.getContextPath() != null && path.startsWith(request.getContextPath())) {
            path = path.substring(request.getContextPath().length());
        }
        if (!path.startsWith("/api/")) {
            return true;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            return true;
        }
        String apiKey = extractApiKey(request);
        return apiKey == null || apiKey.isBlank();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawKey = extractApiKey(request);
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ApiKeyService.ApiKeyAuthResult> result = apiKeyService.validate(rawKey);
        if (result.isEmpty()) {
            log.debug("Invalid or expired API key");
            filterChain.doFilter(request, response);
            return;
        }

        ApiKeyService.ApiKeyAuthResult authResult = result.get();
        TenantContext.setCurrentTenantId(authResult.tenantId());
        userRepository.findByIdAndTenantId(authResult.userId(), authResult.tenantId())
                .ifPresent(user -> {
                    user.getRoles().forEach(role -> role.getPermissions().size());
                    ErpUserDetails details = new ErpUserDetails(user);
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
                });

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    private String extractApiKey(HttpServletRequest request) {
        String key = request.getHeader(HEADER_API_KEY);
        if (key != null && !key.isBlank()) {
            return key.trim();
        }
        String auth = request.getHeader(HEADER_AUTH);
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            return auth.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
