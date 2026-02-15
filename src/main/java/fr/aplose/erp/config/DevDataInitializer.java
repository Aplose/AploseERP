package fr.aplose.erp.config;

import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs at startup in dev profile.
 * Fixes the placeholder BCrypt hash inserted by Flyway migration V2
 * with a runtime-encoded hash using the configured PasswordEncoder.
 *
 * Default credentials: admin / Admin1234!
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private static final String DEV_TENANT_ID = "00000000-0000-0000-0000-000000000001";
    private static final String ADMIN_USERNAME = "admin";
    private static final String DEV_PASSWORD   = "Admin1234!";
    // Placeholder written by Flyway migration — will never match a real BCrypt output
    private static final String PLACEHOLDER    = "$2a$12$OdQb9rvBWvIGlJFD5.YwmOCWmhh4CX/g9X5k5vH4J3z5z4R5z4R5.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository
                .findByUsernameAndTenantIdAndDeletedAtIsNull(ADMIN_USERNAME, DEV_TENANT_ID)
                .ifPresent(admin -> {
                    if (PLACEHOLDER.equals(admin.getPasswordHash())) {
                        admin.setPasswordHash(passwordEncoder.encode(DEV_PASSWORD));
                        userRepository.save(admin);
                        log.info("Dev admin password initialized. Login: {} / {}", ADMIN_USERNAME, DEV_PASSWORD);
                    }
                });
    }
}
