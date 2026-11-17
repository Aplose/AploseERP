package fr.aplose.erp.i18n;

import fr.aplose.erp.i18n.service.ReferenceDataService;
import fr.aplose.erp.mail.service.EmailTemplateService;
import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.service.InvoiceService;
import fr.aplose.erp.modules.commerce.web.dto.InvoiceDto;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.support.TestTenants;
import fr.aplose.erp.tenant.entity.Tenant;
import fr.aplose.erp.tenant.filter.TenantResolutionFilter;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.service.TenantRegistrationService;
import fr.aplose.erp.tenant.web.dto.SignupDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * A company from any country: its currency, language and time zone, and the interface language.
 */
@SpringBootTest
@ActiveProfiles("test")
class InternationalTest {

    @Autowired ReferenceDataService referenceData;
    @Autowired TenantRegistrationService registration;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ThirdPartyRepository thirdPartyRepository;
    @Autowired InvoiceService invoiceService;
    @Autowired EmailTemplateService emailTemplates;
    @Autowired WebApplicationContext context;
    @Autowired TenantResolutionFilter tenantResolutionFilter;

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private Tenant signup(String country, String currency, String language, String timezone) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        SignupDto dto = new SignupDto();
        dto.setCompanyName("Company " + country + " " + unique);
        dto.setCompanyEmail("contact@" + unique + ".test");
        dto.setAdminEmail("admin@" + unique + ".test");
        dto.setPassword(TestTenants.PASSWORD);
        dto.setConfirmPassword(TestTenants.PASSWORD);
        dto.setCountryCode(country);
        dto.setDefaultCurrency(currency);
        dto.setDefaultLocale(language);
        dto.setTimezone(timezone);
        return tenantRepository.findById(registration.register(dto)).orElseThrow();
    }

    @Test
    void everyCountryAndCurrencyIsAvailable() {
        assertThat(referenceData.countries()).hasSizeGreaterThan(240);
        assertThat(referenceData.currencies()).extracting(ReferenceDataService.Option::code)
                .contains("EUR", "USD", "JPY", "BRL", "INR", "CNY", "XOF", "MAD", "KRW", "NGN");

        LocaleContextHolder.setLocale(Locale.FRENCH);
        assertThat(referenceData.countryName("DE")).isEqualTo("Allemagne");
        assertThat(referenceData.countries()).anyMatch(o -> o.code().equals("JP") && o.label().equals("Japon"));
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(referenceData.countryName("DE")).isEqualTo("Germany");
    }

    @Test
    void companySettingsComeFromItsCountry() {
        Tenant japan = signup("JP", null, null, null);
        assertThat(japan.getCountryCode()).isEqualTo("JP");
        assertThat(japan.getDefaultCurrency()).isEqualTo("JPY");
        assertThat(japan.getTimezone()).isEqualTo("Asia/Tokyo");
        assertThat(japan.getDefaultLocale()).as("Japanese UI not available yet").isEqualTo("en");

        Tenant france = signup("FR", null, null, null);
        assertThat(france.getDefaultCurrency()).isEqualTo("EUR");
        assertThat(france.getDefaultLocale()).isEqualTo("fr");
        assertThat(france.getTimezone()).isEqualTo("Europe/Paris");

        Tenant brazil = signup("BR", "USD", "en", "America/Sao_Paulo");
        assertThat(brazil.getDefaultCurrency()).as("explicit choice wins").isEqualTo("USD");
        assertThat(brazil.getTimezone()).isEqualTo("America/Sao_Paulo");
        assertThat(userRepository.findAllByLogin(brazil.getEmail().replace("contact@", "admin@")))
                .singleElement().satisfies(admin -> {
                    assertThat(admin.getLocale()).isEqualTo("en");
                    assertThat(admin.getTimezone()).isEqualTo("America/Sao_Paulo");
                });
    }

    @Test
    void documentsUseTheCompanysCurrencyByDefault() {
        Tenant japan = signup("JP", null, null, null);
        TestTenants.TestTenant t = new TestTenants.TestTenant(japan.getId(), japan.getCode(), null, null);
        Invoice inv = TestTenants.as(t, () -> {
            var customer = TestTenants.customer(thirdPartyRepository, "JP-1", "Tokyo Trading");
            InvoiceDto dto = new InvoiceDto();
            dto.setThirdPartyId(customer.getId());
            dto.setDateIssued(LocalDate.now());
            dto.setDateDue(LocalDate.now().plusDays(30));
            return invoiceService.create(dto, null);
        });
        assertThat(inv.getCurrencyCode()).isEqualTo("JPY");
    }

    @Test
    void emailsInTheRecipientsLanguage() {
        assertThat(emailTemplates.findBest("welcome", "fr").orElseThrow().getSubject()).startsWith("Bienvenue");
        assertThat(emailTemplates.findBest("welcome", "fr-CA").orElseThrow().getSubject()).startsWith("Bienvenue");
        assertThat(emailTemplates.findBest("welcome", "en").orElseThrow().getSubject()).startsWith("Welcome");
        assertThat(emailTemplates.findBest("welcome", "de").orElseThrow().getSubject()).as("fallback").startsWith("Welcome");
        assertThat(emailTemplates.findBest("welcome", null).orElseThrow().getSubject()).startsWith("Welcome");
    }

    @Test
    void interfaceFollowsTheBrowserLanguage() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(tenantResolutionFilter).apply(springSecurity()).build();
        String fr = mvc.perform(get("/login").header("Accept-Language", "fr-FR,fr;q=0.9")).andReturn().getResponse().getContentAsString();
        assertThat(fr).contains("Email ou identifiant");
        String de = mvc.perform(get("/login").header("Accept-Language", "de-DE")).andReturn().getResponse().getContentAsString();
        assertThat(de).contains("Email or username");

        String json = mvc.perform(get("/signup/defaults").param("country", "JP")).andReturn().getResponse().getContentAsString();
        assertThat(json).contains("\"currency\":\"JPY\"").contains("Asia/Tokyo");
    }
}
