package fr.aplose.erp.security;

import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.security.service.UserService;
import fr.aplose.erp.security.web.dto.UserCreateDto;
import fr.aplose.erp.support.TestTenants;
import fr.aplose.erp.support.TestTenants.TestTenant;
import fr.aplose.erp.tenant.filter.TenantResolutionFilter;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.service.TenantRegistrationService;
import fr.aplose.erp.tenant.web.dto.SignupDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import fr.aplose.erp.security.service.ErpUserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * One access point for every company: same host, same login page; the company comes from the account.
 */
@SpringBootTest
@ActiveProfiles("test")
class SingleLoginWebTest {

    private static final String HOST = "erp.aplose.test";

    @Autowired WebApplicationContext context;
    @Autowired TenantResolutionFilter tenantResolutionFilter;
    @Autowired TenantRegistrationService registration;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired UserService userService;

    MockMvc mvc;
    TestTenant alpha;
    TestTenant beta;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(tenantResolutionFilter).apply(springSecurity()).build();
        alpha = TestTenants.register(registration, tenantRepository, userRepository, "Alpha");
        beta = TestTenants.register(registration, tenantRepository, userRepository, "Beta");
    }

    private MvcResult login(String login, String password) throws Exception {
        return mvc.perform(post("/login").with(csrf()).with(r -> { r.setServerName(HOST); return r; })
                .param("username", login).param("password", password)).andReturn();
    }

    private static String tenantOf(MvcResult result) {
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        SecurityContext ctx = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        return ((ErpUserDetails) ctx.getAuthentication().getPrincipal()).getTenantId();
    }

    @Test
    void everyCompanyLogsInOnTheSamePage() throws Exception {
        MvcResult a = login(alpha.adminUsername(), TestTenants.PASSWORD);
        assertThat(a.getResponse().getRedirectedUrl()).endsWith("/dashboard");
        assertThat(tenantOf(a)).isEqualTo(alpha.id());

        MvcResult b = login(beta.adminUsername().toUpperCase(), TestTenants.PASSWORD);
        assertThat(b.getResponse().getRedirectedUrl()).as("email is case-insensitive").endsWith("/dashboard");
        assertThat(tenantOf(b)).isEqualTo(beta.id());

        assertThat(login(alpha.adminUsername(), "wrong-password").getResponse().getRedirectedUrl()).endsWith("/login?error");
    }

    @Test
    void usersCreatedByTheAdminBelongToItsCompany() throws Exception {
        String email = "employee-" + System.nanoTime() + "@alpha.test";
        TestTenants.run(alpha, () -> {
            UserCreateDto dto = new UserCreateDto();
            dto.setUsername("employee" + System.nanoTime());
            dto.setEmail(email);
            dto.setPassword("Employee-Pass-1");
            dto.setConfirmPassword("Employee-Pass-1");
            userService.create(dto);
        });

        MvcResult r = login(email, "Employee-Pass-1");
        assertThat(r.getResponse().getRedirectedUrl()).endsWith("/dashboard");
        assertThat(tenantOf(r)).isEqualTo(alpha.id());
    }

    @Test
    void anEmailBelongsToOneAccountOnly() {
        // Another company cannot reuse an existing email, neither on signup nor when creating a user
        SignupDto dto = new SignupDto();
        dto.setCompanyName("Gamma");
        dto.setCompanyEmail("contact@gamma.test");
        dto.setAdminEmail(alpha.adminUsername());
        dto.setPassword(TestTenants.PASSWORD);
        dto.setConfirmPassword(TestTenants.PASSWORD);
        assertThatThrownBy(() -> registration.register(dto)).isInstanceOf(IllegalStateException.class);

        TestTenants.run(beta, () -> {
            UserCreateDto user = new UserCreateDto();
            user.setUsername("someone" + System.nanoTime());
            user.setEmail(alpha.adminUsername());
            user.setPassword("Some-Pass-123");
            user.setConfirmPassword("Some-Pass-123");
            assertThatThrownBy(() -> userService.create(user)).isInstanceOf(IllegalStateException.class);
        });
    }
}
