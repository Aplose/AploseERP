package fr.aplose.erp.security.service;

import fr.aplose.erp.security.entity.User;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.entity.Tenant;
import fr.aplose.erp.tenant.repository.TenantRepository;
import org.springframework.security.authentication.DisabledException;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    /**
     * Single login page for every company: the user is found by email or username across all
     * companies, and the company is the user's own. Identifiers are unique platform-wide
     * (enforced on signup and user creation); an identifier matching several accounts (data
     * created before this rule) is refused rather than guessed.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String normalized = login != null ? login.trim() : "";
        List<User> matches = userRepository.findAllByLogin(normalized);
        if (matches.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + normalized);
        }
        if (matches.size() > 1) {
            log.error("Login '{}' matches {} accounts in different companies: refused", normalized, matches.size());
            throw new UsernameNotFoundException("Ambiguous login: " + normalized);
        }
        User user = matches.get(0);
        boolean companyActive = tenantRepository.findById(user.getTenantId()).map(Tenant::isActive).orElse(false);
        if (!companyActive) {
            throw new DisabledException("Company disabled for user " + normalized);
        }

        // Eagerly initialize roles and permissions within this transaction
        user.getRoles().forEach(role -> role.getPermissions().size());

        log.debug("Loaded user: {} for tenant: {}", user.getUsername(), user.getTenantId());
        return new ErpUserDetails(user);
    }

    /**
     * Loads a user by id within its tenant, roles and permissions included.
     * Used by the API key filter, which runs outside any open persistence session.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<ErpUserDetails> loadByIdAndTenant(Long userId, String tenantId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .filter(user -> user.getDeletedAt() == null)
                .map(user -> {
                    user.getRoles().forEach(role -> role.getPermissions().size());
                    return new ErpUserDetails(user);
                });
    }
}
