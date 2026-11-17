package fr.aplose.erp.security;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.service.InvoiceService;
import fr.aplose.erp.modules.commerce.web.dto.InvoiceDto;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.crm.entity.CrmActivity;
import fr.aplose.erp.modules.crm.repository.CrmActivityRepository;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.security.service.ApiKeyService;
import fr.aplose.erp.support.TestTenants;
import fr.aplose.erp.support.TestTenants.TestTenant;
import fr.aplose.erp.tenant.filter.TenantResolutionFilter;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.service.TenantRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * A user logged in to one company must never reach another company's data, whatever the request
 * says about the tenant (sub-domain, X-Tenant-ID header).
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationWebTest {

    @Autowired WebApplicationContext context;
    @Autowired TenantResolutionFilter tenantResolutionFilter;
    MockMvc mvc;
    @Autowired TenantRegistrationService registration;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ThirdPartyRepository thirdPartyRepository;
    @Autowired InvoiceService invoiceService;
    @Autowired ApiKeyService apiKeyService;
    @Autowired CrmActivityRepository activityRepository;

    TestTenant alpha;
    TestTenant beta;
    Invoice betaInvoice;
    ThirdParty betaCustomer;

    @BeforeEach
    void setUp() {
        // The tenant filter is a plain servlet filter: MockMvc only runs it when added explicitly, before security
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(tenantResolutionFilter).apply(springSecurity()).build();
        alpha = TestTenants.register(registration, tenantRepository, userRepository, "Alpha");
        beta = TestTenants.register(registration, tenantRepository, userRepository, "Beta");
        betaCustomer = TestTenants.as(beta, () -> TestTenants.customer(thirdPartyRepository, "BETA-SECRET-CUSTOMER", "Client secret de Beta"));
        betaInvoice = TestTenants.as(beta, () -> {
            InvoiceDto dto = new InvoiceDto();
            dto.setThirdPartyId(betaCustomer.getId());
            dto.setDateIssued(LocalDate.now());
            dto.setDateDue(LocalDate.now().plusDays(30));
            Invoice inv = invoiceService.create(dto, beta.adminUserId());
            LineDto line = new LineDto();
            line.setDescription("Prestation confidentielle");
            line.setQuantity(BigDecimal.ONE);
            line.setUnitPrice(new BigDecimal("1234.56"));
            invoiceService.addLine(inv.getId(), line);
            return inv;
        });
    }

    private static RequestPostProcessor host(String serverName) {
        return request -> {
            request.setServerName(serverName);
            return request;
        };
    }

    private MockHttpSession login(TestTenant tenant) throws Exception {
        MvcResult result = mvc.perform(post("/login").with(csrf()).with(host(tenant.host()))
                        .param("username", tenant.adminUsername())
                        .param("password", TestTenants.PASSWORD))
                .andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).as("login of %s", tenant.code()).endsWith("/dashboard");
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /** Status and body of the response; an exception escaping the controller counts as a refusal. */
    private String body(MockHttpServletRequestBuilder request) throws Exception {
        try {
            MvcResult r = mvc.perform(request).andReturn();
            return r.getResponse().getStatus() + "\n" + r.getResponse().getContentAsString();
        } catch (jakarta.servlet.ServletException e) {
            return "500\n" + e.getMessage();
        }
    }

    @Test
    void loggedInUserSeesOwnCompanyOnly() throws Exception {
        MockHttpSession session = login(alpha);
        String list = body(get("/invoices").session(session).with(host(alpha.host())));
        assertThat(list).startsWith("200").doesNotContain(betaInvoice.getReference() + "</a>").doesNotContain("Client secret de Beta");
    }

    @Test
    void anotherSubdomainDoesNotSwitchCompany() throws Exception {
        MockHttpSession session = login(alpha);

        // Same session cookie sent to beta's sub-domain (trivial with curl: the cookie is just a header)
        String detail = body(get("/invoices/" + betaInvoice.getId()).session(session).with(host(beta.host())));
        assertThat(detail).doesNotContain("Prestation confidentielle").doesNotContain("Client secret de Beta");

        String customers = body(get("/third-parties").session(session).with(host(beta.host())));
        assertThat(customers).doesNotContain("BETA-SECRET-CUSTOMER");
    }

    @Test
    void tenantHeaderDoesNotSwitchCompany() throws Exception {
        MockHttpSession session = login(alpha);

        String detail = body(get("/invoices/" + betaInvoice.getId()).session(session)
                .with(host("localhost")).header("X-Tenant-ID", beta.id()));
        assertThat(detail).doesNotContain("Prestation confidentielle").doesNotContain("Client secret de Beta");
    }

    @Test
    void apiKeyIsBoundToItsCompany() throws Exception {
        String alphaKey = apiKeyService.create(alpha.id(), alpha.adminUserId(), "test");

        String detail = body(get("/api/v1/invoices/" + betaInvoice.getId())
                .with(host("localhost")).header("X-API-Key", alphaKey).header("X-Tenant-ID", beta.id()));
        assertThat(detail).doesNotStartWith("200").doesNotContain(betaInvoice.getReference());

        String list = body(get("/api/v1/third-parties").with(host(beta.host())).header("X-API-Key", alphaKey));
        assertThat(list).doesNotContain("BETA-SECRET-CUSTOMER");
    }

    @Test
    void ownerStillReachesOwnInvoice() throws Exception {
        MockHttpSession session = login(beta);
        String detail = body(get("/invoices/" + betaInvoice.getId()).session(session).with(host(beta.host())));
        assertThat(detail).startsWith("200").contains("Prestation confidentielle");
    }

    @Test
    void creationFormCannotOverwriteAnotherCompanysRecord() throws Exception {
        CrmActivity betaActivity = TestTenants.as(beta, () -> {
            CrmActivity a = new CrmActivity();
            a.setThirdParty(betaCustomer);
            a.setActivityType("CALL");
            a.setSubject("Appel confidentiel Beta");
            return activityRepository.save(a);
        });
        ThirdParty alphaCustomer = TestTenants.as(alpha, () -> TestTenants.customer(thirdPartyRepository, "ALPHA-1", "Client Alpha"));
        MockHttpSession session = login(alpha);

        // Mass assignment attempt: a creation form carrying the id of Beta's activity
        body(post("/crm/activities").session(session).with(csrf()).with(host(alpha.host()))
                .param("id", String.valueOf(betaActivity.getId()))
                .param("tenantId", beta.id())
                .param("thirdPartyId", String.valueOf(alphaCustomer.getId()))
                .param("activityType", "CALL")
                .param("subject", "Ecrasé par Alpha"));

        CrmActivity after = activityRepository.findById(betaActivity.getId()).orElseThrow();
        assertThat(after.getSubject()).isEqualTo("Appel confidentiel Beta");
        assertThat(after.getTenantId()).isEqualTo(beta.id());
    }
}
