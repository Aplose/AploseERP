package fr.aplose.erp.tenant.service;

import fr.aplose.erp.security.entity.Permission;
import fr.aplose.erp.security.entity.Role;
import fr.aplose.erp.security.entity.User;
import fr.aplose.erp.security.repository.PermissionRepository;
import fr.aplose.erp.security.repository.RoleRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.entity.Tenant;
import fr.aplose.erp.tenant.plan.SubscriptionPlan;
import fr.aplose.erp.mail.service.MailService;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.web.dto.SignupDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantRegistrationService {

    private static final Pattern SLUG_ALLOW = Pattern.compile("[^a-z0-9-]");
    private static final int MAX_CODE_LENGTH = 50;

    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final DictionaryService dictionaryService;
    private final TenantModuleService tenantModuleService;

    @Transactional
    public String register(SignupDto dto) {
        String planCode = SubscriptionPlan.fromCode(dto.getPlan()).getCode();
        String tenantId = UUID.randomUUID().toString();
        String code = uniqueTenantCode(slug(dto.getCompanyCode() != null && !dto.getCompanyCode().isBlank()
                ? dto.getCompanyCode()
                : dto.getCompanyName()));

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setCode(code);
        tenant.setName(dto.getCompanyName().trim());
        tenant.setEmail(dto.getCompanyEmail().trim().toLowerCase());
        tenant.setPhone(dto.getCompanyPhone() != null ? dto.getCompanyPhone().trim() : null);
        tenant.setAddressLine1(trimToNull(dto.getAddressLine1()));
        tenant.setAddressLine2(trimToNull(dto.getAddressLine2()));
        tenant.setPostalCode(trimToNull(dto.getPostalCode()));
        tenant.setCity(trimToNull(dto.getCity()));
        tenant.setStateProvince(trimToNull(dto.getStateProvince()));
        tenant.setCountryCode(trimToNull(dto.getCountryCode()));
        tenant.setRegistrationId(trimToNull(dto.getRegistrationId()));
        tenant.setPlan(planCode);
        tenant.setDefaultLocale("fr");
        tenant.setDefaultCurrency("EUR");
        tenant.setTimezone("Europe/Paris");
        tenant.setActive(true);
        tenantRepository.save(tenant);

        List<Permission> allPermissions = permissionRepository.findAllByOrderByModuleAscActionAsc();
        Role adminRole = new Role();
        adminRole.setTenantId(tenantId);
        adminRole.setCode("TENANT_ADMIN");
        adminRole.setName("Administrateur");
        adminRole.setDescription("Full access for tenant administrator");
        adminRole.setSystem(true);
        adminRole.setPermissions(new HashSet<>(allPermissions));
        roleRepository.save(adminRole);

        String username = dto.getAdminEmail().trim().toLowerCase();
        username = uniqueUsername(tenantId, username);

        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(dto.getAdminEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getAdminFirstName() != null ? dto.getAdminFirstName().trim() : null);
        user.setLastName(dto.getAdminLastName() != null ? dto.getAdminLastName().trim() : null);
        user.setActive(true);
        user.setTenantAdmin(true);
        user.setRoles(Set.of(adminRole));
        userRepository.save(user);

        try {
            String displayName = (dto.getAdminFirstName() != null ? dto.getAdminFirstName() + " " : "") + (dto.getAdminLastName() != null ? dto.getAdminLastName() : "");
            if (displayName.isBlank()) displayName = username;
            String adminEmail = dto.getAdminEmail().trim().toLowerCase();
            mailService.sendWelcome(adminEmail, displayName, dto.getCompanyName().trim());
            try {
                mailService.sendOnboarding(adminEmail, displayName);
            } catch (Exception e2) {
                log.warn("Could not send onboarding email: {}", e2.getMessage());
            }
        } catch (Exception e) {
            log.warn("Could not send welcome email: {}", e.getMessage());
        }

        try {
            dictionaryService.seedDefaultsForTenant(tenantId);
        } catch (Exception e) {
            log.warn("Could not seed dictionary for tenant {}: {}", tenantId, e.getMessage());
        }

        try {
            tenantModuleService.ensureTenantHasModuleRows(tenantId);
        } catch (Exception e) {
            log.warn("Could not seed tenant modules for tenant {}: {}", tenantId, e.getMessage());
        }

        log.info("Registered new tenant: {} ({}), plan: {}, admin: {}", code, tenantId, planCode, username);
        return tenantId;
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String slug(String name) {
        if (name == null || name.isBlank()) return "company";
        String s = name.trim().toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[àáâãäåæ]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("ç", "c");
        s = SLUG_ALLOW.matcher(s).replaceAll("");
        if (s.length() > MAX_CODE_LENGTH) s = s.substring(0, MAX_CODE_LENGTH);
        return s.isEmpty() ? "company" : s;
    }

    private String uniqueTenantCode(String base) {
        String candidate = base;
        int n = 1;
        while (tenantRepository.findByCode(candidate).isPresent()) {
            String suffix = "-" + (n++);
            candidate = (base.length() + suffix.length() > MAX_CODE_LENGTH)
                    ? base.substring(0, MAX_CODE_LENGTH - suffix.length()) + suffix
                    : base + suffix;
        }
        return candidate;
    }

    private String uniqueUsername(String tenantId, String base) {
        String candidate = base;
        int n = 1;
        while (userRepository.findByUsernameAndTenantIdAndDeletedAtIsNull(candidate, tenantId).isPresent()) {
            candidate = base + n++;
        }
        return candidate;
    }
}
