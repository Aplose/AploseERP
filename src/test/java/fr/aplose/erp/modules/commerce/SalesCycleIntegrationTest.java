package fr.aplose.erp.modules.commerce;

import fr.aplose.erp.modules.catalog.entity.Product;
import fr.aplose.erp.modules.catalog.repository.ProductRepository;
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
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.support.TestTenants;
import fr.aplose.erp.support.TestTenants.TestTenant;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.service.TenantRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The whole sales cycle on the service layer: proposal -> order -> invoice -> payments.
 */
@SpringBootTest
@ActiveProfiles("test")
class SalesCycleIntegrationTest {

    @Autowired TenantRegistrationService registration;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ThirdPartyRepository thirdPartyRepository;
    @Autowired ProductRepository productRepository;
    @Autowired ProposalService proposalService;
    @Autowired SalesOrderService orderService;
    @Autowired InvoiceService invoiceService;
    @Autowired TransactionTemplate tx;

    TestTenant company;
    ThirdParty customer;
    Product bread;

    @BeforeEach
    void setUp() {
        company = TestTenants.register(registration, tenantRepository, userRepository, "Boulangerie");
        customer = TestTenants.as(company, () -> TestTenants.customer(thirdPartyRepository, "CU-1", "Hôtel du Lac"));
        bread = TestTenants.as(company, () -> {
            Product p = new Product();
            p.setCode("PAIN");
            p.setName("Pain de campagne");
            p.setSalePrice(new BigDecimal("50"));
            p.setVatRate(new BigDecimal("0.055"));
            return productRepository.save(p);
        });
    }

    private static LineDto line(String description, String qty, String unitPrice, String discountPct, String vatRate, Long productId) {
        LineDto l = new LineDto();
        l.setDescription(description);
        l.setQuantity(new BigDecimal(qty));
        l.setUnitPrice(new BigDecimal(unitPrice));
        l.setDiscountPct(new BigDecimal(discountPct));
        l.setVatRate(new BigDecimal(vatRate));
        l.setProductId(productId);
        return l;
    }

    private static PaymentDto payment(String amount) {
        PaymentDto p = new PaymentDto();
        p.setAmount(new BigDecimal(amount));
        p.setPaymentDate(LocalDate.now());
        p.setPaymentMethod("VIR");
        return p;
    }

    private Proposal draftProposalWithTwoLines() {
        ProposalDto dto = new ProposalDto();
        dto.setThirdPartyId(customer.getId());
        dto.setTitle("Livraisons de janvier");
        dto.setDateIssued(LocalDate.now());
        Proposal p = proposalService.create(dto, company.adminUserId());
        // 2 x 100 with 10 % discount, VAT 20 % -> 180 + 36
        proposalService.addLine(p.getId(), line("Viennoiseries", "2", "100", "0.10", "0.20", null));
        // 1 x 50, VAT 5.5 % -> 50 + 2.75
        proposalService.addLine(p.getId(), line("Pain", "1", "50", "0", "0.055", bread.getId()));
        return proposalService.findById(p.getId());
    }

    @Test
    void proposalToOrderToInvoiceToPayment() {
        TestTenants.run(company, () -> {
            // 1. Proposal
            Proposal proposal = draftProposalWithTwoLines();
            assertThat(proposal.getSubtotal()).isEqualByComparingTo("230");
            assertThat(proposal.getVatAmount()).isEqualByComparingTo("38.75");
            assertThat(proposal.getTotalAmount()).isEqualByComparingTo("268.75");

            assertThatThrownBy(() -> orderService.createFromProposal(proposal.getId(), company.adminUserId()))
                    .as("a draft proposal cannot become an order").isInstanceOf(IllegalStateException.class);

            // 2. Order
            proposalService.updateStatus(proposal.getId(), "ACCEPTED");
            SalesOrder order = orderService.createFromProposal(proposal.getId(), company.adminUserId());
            assertThat(order.getTotalAmount()).isEqualByComparingTo("268.75");
            assertThat(proposalService.findById(proposal.getId()).getStatus()).isEqualTo("CONVERTED");
            assertThatThrownBy(() -> orderService.createFromProposal(proposal.getId(), company.adminUserId()))
                    .as("a proposal is converted only once").isInstanceOf(IllegalStateException.class);

            // 3. Invoice
            Invoice invoice = invoiceService.createFromOrder(order.getId(), company.adminUserId());
            assertThat(invoice.getStatus()).isEqualTo("DRAFT");
            assertThat(invoice.getReference()).isEqualTo("INV-00001");
            assertThat(invoice.getType()).isEqualTo("SALES");
            assertThat(invoice.getSubtotal()).isEqualByComparingTo("230");
            assertThat(invoice.getVatAmount()).isEqualByComparingTo("38.75");
            assertThat(invoice.getTotalAmount()).isEqualByComparingTo("268.75");
            assertThat(invoice.getAmountRemaining()).isEqualByComparingTo("268.75");
            assertThat(invoice.getDateDue()).isEqualTo(LocalDate.now().plusDays(30));
            tx.executeWithoutResult(s -> {
                Invoice reloaded = invoiceService.findById(invoice.getId());
                assertThat(reloaded.getSalesOrder().getId()).isEqualTo(order.getId());
                assertThat(reloaded.getThirdParty().getId()).isEqualTo(customer.getId());
                assertThat(reloaded.getLines()).hasSize(2);
                assertThat(reloaded.getLines()).anyMatch(l -> l.getProduct() != null && l.getProduct().getId().equals(bread.getId()));
            });
            assertThatThrownBy(() -> invoiceService.createFromOrder(order.getId(), company.adminUserId()))
                    .as("an order is invoiced only once").isInstanceOf(IllegalStateException.class);

            // 4. Payments
            assertThatThrownBy(() -> invoiceService.addPayment(invoice.getId(), payment("100"), company.adminUserId()))
                    .as("no payment on a draft").isInstanceOf(IllegalStateException.class);

            invoiceService.validate(invoice.getId(), company.adminUserId());
            assertThat(invoiceService.findById(invoice.getId()).getStatus()).isEqualTo("VALIDATED");

            invoiceService.addPayment(invoice.getId(), payment("100"), company.adminUserId());
            Invoice partly = invoiceService.findById(invoice.getId());
            assertThat(partly.getStatus()).isEqualTo("PARTIALLY_PAID");
            assertThat(partly.getAmountPaid()).isEqualByComparingTo("100");
            assertThat(partly.getAmountRemaining()).isEqualByComparingTo("168.75");

            invoiceService.addPayment(invoice.getId(), payment("168.75"), company.adminUserId());
            Invoice paid = invoiceService.findById(invoice.getId());
            assertThat(paid.getStatus()).isEqualTo("PAID");
            assertThat(paid.getAmountRemaining()).isEqualByComparingTo("0");
        });
    }

    @Test
    void invoiceRules() {
        TestTenants.run(company, () -> {
            InvoiceDto dto = new InvoiceDto();
            dto.setThirdPartyId(customer.getId());
            dto.setDateIssued(LocalDate.now());
            dto.setDateDue(LocalDate.now().plusDays(30));
            Invoice empty = invoiceService.create(dto, company.adminUserId());

            assertThatThrownBy(() -> invoiceService.validate(empty.getId(), company.adminUserId()))
                    .as("no validation without lines").isInstanceOf(IllegalStateException.class);

            invoiceService.addLine(empty.getId(), line("Service", "1", "10", "0", "0.20", null));
            invoiceService.validate(empty.getId(), company.adminUserId());
            assertThatThrownBy(() -> invoiceService.addLine(empty.getId(), line("Ajout", "1", "1", "0", "0", null)))
                    .as("a validated invoice is frozen").isInstanceOf(IllegalStateException.class);

            Invoice second = invoiceService.create(dto, company.adminUserId());
            assertThat(second.getReference()).as("numbering is sequential").isEqualTo("INV-00002");
        });
    }

    @Test
    void cancelledOrderCannotBeInvoiced() {
        TestTenants.run(company, () -> {
            Proposal proposal = draftProposalWithTwoLines();
            proposalService.updateStatus(proposal.getId(), "ACCEPTED");
            SalesOrder order = orderService.createFromProposal(proposal.getId(), company.adminUserId());
            orderService.updateStatus(order.getId(), "CANCELLED");

            assertThatThrownBy(() -> invoiceService.createFromOrder(order.getId(), company.adminUserId()))
                    .isInstanceOf(IllegalStateException.class);
        });
    }
}
