package fr.aplose.erp.security;

import fr.aplose.erp.modules.catalog.entity.Product;
import fr.aplose.erp.modules.catalog.repository.ProductRepository;
import fr.aplose.erp.modules.catalog.service.ProductService;
import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.entity.SalesOrder;
import fr.aplose.erp.modules.commerce.service.InvoiceService;
import fr.aplose.erp.modules.commerce.service.ProposalService;
import fr.aplose.erp.modules.commerce.service.SalesOrderService;
import fr.aplose.erp.modules.commerce.web.dto.InvoiceDto;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.commerce.web.dto.PaymentDto;
import fr.aplose.erp.modules.commerce.web.dto.ProposalDto;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.contact.service.ContactService;
import fr.aplose.erp.modules.project.entity.Project;
import fr.aplose.erp.modules.project.repository.ProjectRepository;
import fr.aplose.erp.modules.project.service.ProjectService;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.modules.thirdparty.service.ThirdPartyService;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.support.TestTenants;
import fr.aplose.erp.support.TestTenants.TestTenant;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.service.TenantRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Services must refuse, for the current company, any id belonging to another company:
 * reading, modifying, converting or paying it.
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationServiceTest {

    @Autowired TenantRegistrationService registration;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ThirdPartyRepository thirdPartyRepository;
    @Autowired ContactRepository contactRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ThirdPartyService thirdPartyService;
    @Autowired ContactService contactService;
    @Autowired ProductService productService;
    @Autowired ProjectService projectService;
    @Autowired ProposalService proposalService;
    @Autowired SalesOrderService orderService;
    @Autowired InvoiceService invoiceService;

    TestTenant alpha;
    TestTenant beta;
    ThirdParty alphaCustomer;
    Contact alphaContact;
    Product alphaProduct;
    Project alphaProject;
    Proposal alphaProposal;
    SalesOrder alphaOrder;
    Invoice alphaInvoice;

    @BeforeEach
    void setUp() {
        alpha = TestTenants.register(registration, tenantRepository, userRepository, "Alpha");
        beta = TestTenants.register(registration, tenantRepository, userRepository, "Beta");
        TestTenants.run(alpha, () -> {
            alphaCustomer = TestTenants.customer(thirdPartyRepository, "A-1", "Client Alpha");
            Contact c = new Contact();
            c.setFirstName("Alice");
            c.setLastName("Alpha");
            alphaContact = contactRepository.save(c);
            Product p = new Product();
            p.setCode("A-PROD");
            p.setName("Produit Alpha");
            alphaProduct = productRepository.save(p);
            Project pj = new Project();
            pj.setCode("A-PJ");
            pj.setName("Projet Alpha");
            alphaProject = projectRepository.save(pj);

            ProposalDto pd = new ProposalDto();
            pd.setThirdPartyId(alphaCustomer.getId());
            pd.setDateIssued(LocalDate.now());
            alphaProposal = proposalService.create(pd, alpha.adminUserId());
            proposalService.addLine(alphaProposal.getId(), line());
            proposalService.updateStatus(alphaProposal.getId(), "ACCEPTED");
            alphaOrder = orderService.createFromProposal(alphaProposal.getId(), alpha.adminUserId());
            alphaInvoice = invoiceService.createFromOrder(alphaOrder.getId(), alpha.adminUserId());
            invoiceService.validate(alphaInvoice.getId(), alpha.adminUserId());
        });
    }

    private static LineDto line() {
        LineDto l = new LineDto();
        l.setDescription("Prestation");
        l.setQuantity(BigDecimal.ONE);
        l.setUnitPrice(new BigDecimal("100"));
        return l;
    }

    @Test
    void readingAnotherCompanysRecordsIsRefused() {
        TestTenants.run(beta, () -> {
            assertThatThrownBy(() -> thirdPartyService.findById(alphaCustomer.getId())).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> contactService.findById(alphaContact.getId())).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> productService.findById(alphaProduct.getId())).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> projectService.findById(alphaProject.getId())).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> proposalService.findById(alphaProposal.getId())).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> orderService.findById(alphaOrder.getId())).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> invoiceService.findById(alphaInvoice.getId())).isInstanceOf(RuntimeException.class);
        });
    }

    @Test
    void listsOnlyShowOwnRecords() {
        TestTenants.run(beta, () -> {
            assertThat(thirdPartyService.findAll(null, null, Pageable.ofSize(100)).getContent())
                    .noneMatch(t -> t.getId().equals(alphaCustomer.getId()));
            assertThat(proposalService.findAll(null, null, Pageable.ofSize(100)).getContent()).isEmpty();
            assertThat(orderService.findAll(null, null, Pageable.ofSize(100)).getContent()).isEmpty();
            assertThat(invoiceService.findAll(null, null, null, Pageable.ofSize(100)).getContent()).isEmpty();
            assertThat(invoiceService.findAll("Alpha", null, null, Pageable.ofSize(100)).getContent()).isEmpty();
        });
    }

    @Test
    void actingOnAnotherCompanysRecordsIsRefused() {
        TestTenants.run(beta, () -> {
            assertThatThrownBy(() -> orderService.createFromProposal(alphaProposal.getId(), beta.adminUserId()))
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> invoiceService.createFromOrder(alphaOrder.getId(), beta.adminUserId()))
                    .isInstanceOf(RuntimeException.class);
            PaymentDto pay = new PaymentDto();
            pay.setAmount(new BigDecimal("100"));
            pay.setPaymentDate(LocalDate.now());
            pay.setPaymentMethod("VIR");
            assertThatThrownBy(() -> invoiceService.addPayment(alphaInvoice.getId(), pay, beta.adminUserId()))
                    .isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> invoiceService.cancel(alphaInvoice.getId())).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(() -> proposalService.addLine(alphaProposal.getId(), line())).isInstanceOf(RuntimeException.class);
        });
        TestTenants.run(alpha, () -> {
            Invoice untouched = invoiceService.findById(alphaInvoice.getId());
            assertThat(untouched.getStatus()).isEqualTo("VALIDATED");
            assertThat(untouched.getAmountPaid()).isEqualByComparingTo("0");
        });
    }

    @Test
    void anotherCompanysCustomerCannotBeAttachedToADocument() {
        TestTenants.run(beta, () -> {
            InvoiceDto dto = new InvoiceDto();
            dto.setThirdPartyId(alphaCustomer.getId());
            dto.setDateIssued(LocalDate.now());
            dto.setDateDue(LocalDate.now().plusDays(30));
            try {
                Invoice inv = invoiceService.create(dto, beta.adminUserId());
                assertThat(inv.getThirdParty()).isNull();
            } catch (RuntimeException refused) {
                // refused outright: fine too
            }
        });
    }
}
