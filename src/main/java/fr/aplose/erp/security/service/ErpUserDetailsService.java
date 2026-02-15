package fr.aplose.erp.security.service;

import fr.aplose.erp.security.entity.User;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
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

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new UsernameNotFoundException("No tenant context available");
        }

        User user = userRepository
                .findByUsernameAndTenantIdAndDeletedAtIsNull(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username + " in tenant: " + tenantId));

        // Eagerly initialize roles and permissions within this transaction
        user.getRoles().forEach(role -> role.getPermissions().size());

        log.debug("Loaded user: {} for tenant: {}", username, tenantId);
        return new ErpUserDetails(user);
    }
}
