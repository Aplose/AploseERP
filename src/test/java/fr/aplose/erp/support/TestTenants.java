package fr.aplose.erp.support;

import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.entity.User;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.service.TenantRegistrationService;
import fr.aplose.erp.tenant.web.dto.SignupDto;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Test helper: creates real tenants through the signup service, and runs code "as" a tenant.
 */
public final class TestTenants {

    public static final String PASSWORD = "Secret-Pass-123!";

    /** A registered tenant with its administrator. */
    public record TestTenant(String id, String code, String adminUsername, Long adminUserId) {
        public String host() {
            return code + ".erp.test";
        }
    }

    private TestTenants() {}

    public static TestTenant register(TenantRegistrationService registration, TenantRepository tenants,
                                      UserRepository users, String name) {
        String unique = name.toLowerCase() + UUID.randomUUID().toString().substring(0, 8);
        SignupDto dto = new SignupDto();
        dto.setCompanyName(name);
        dto.setCompanyCode(unique);
        dto.setCompanyEmail("contact@" + unique + ".test");
        dto.setAdminEmail("admin@" + unique + ".test");
        dto.setAdminFirstName("Admin");
        dto.setAdminLastName(name);
        dto.setPassword(PASSWORD);
        dto.setConfirmPassword(PASSWORD);
        String tenantId = registration.register(dto);
        String code = tenants.findById(tenantId).orElseThrow().getCode();
        User admin = users.findByUsernameAndTenantIdAndDeletedAtIsNull(dto.getAdminEmail(), tenantId).orElseThrow();
        return new TestTenant(tenantId, code, admin.getUsername(), admin.getId());
    }

    /** Runs the action with the given tenant as current tenant. */
    public static <T> T as(TestTenant tenant, Supplier<T> action) {
        String previous = TenantContext.getCurrentTenantId();
        TenantContext.setCurrentTenantId(tenant.id());
        try {
            return action.get();
        } finally {
            if (previous != null) TenantContext.setCurrentTenantId(previous);
            else TenantContext.clear();
        }
    }

    public static void run(TestTenant tenant, Runnable action) {
        as(tenant, () -> {
            action.run();
            return null;
        });
    }

    /** Saves a customer for the current tenant. */
    public static ThirdParty customer(ThirdPartyRepository repo, String code, String name) {
        ThirdParty tp = new ThirdParty();
        tp.setCode(code);
        tp.setName(name);
        tp.setType("CUSTOMER");
        tp.setCustomer(true);
        return repo.save(tp);
    }
}
