package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.dolibarr.entity.DolibarrImportRun;
import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.entity.Payment;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.commerce.repository.PaymentRepository;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.contact.repository.ContactThirdPartyLinkRepository;
import fr.aplose.erp.modules.catalog.repository.ProductRepository;
import fr.aplose.erp.modules.project.entity.Project;
import fr.aplose.erp.modules.project.entity.ProjectTask;
import fr.aplose.erp.modules.project.repository.ProjectRepository;
import fr.aplose.erp.modules.project.repository.ProjectTaskRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the whole Dolibarr import against a fake Dolibarr API, twice, and checks that the second
 * run creates nothing new while every link (contact → third party, task → project...) is kept.
 */
@SpringBootTest
@ActiveProfiles("test")
class DolibarrImportIntegrationTest {

    private static final String TENANT = "00000000-0000-0000-0000-000000000001";

    /** 15 March 2026 at noon, Paris time, as Dolibarr sends it: a Unix timestamp. */
    private static final long MARCH_15 = LocalDate.of(2026, 3, 15).atTime(12, 0)
            .atZone(ZoneId.systemDefault()).toEpochSecond();

    @TestConfiguration
    static class FakeDolibarrConfig {
        @Bean
        @Primary
        DolibarrApiClient fakeDolibarrApiClient() {
            return new FakeDolibarr();
        }
    }

    /** In-memory Dolibarr answering the resources the import reads. */
    static class FakeDolibarr implements DolibarrApiClient {
        final Map<String, List<Map<String, Object>>> data = new HashMap<>();

        FakeDolibarr() {
            data.put("thirdparties", List.of(
                    Map.of("id", 10, "name", "Boulangerie Durand", "code_client", "CU2601-0001", "client", "1", "town", "Juvignac")));
            data.put("contacts", List.of(
                    Map.of("id", 20, "firstname", "Paul", "lastname", "Durand", "socid", 10)));
            data.put("products", List.of(
                    Map.of("id", 30, "ref", "PAIN-01", "label", "Pain de campagne", "price", "3.50", "tva_tx", "5.5", "type", "0")));
            data.put("invoices", List.of(
                    map("id", 40, "ref", "FA2603-0001", "socid", 10, "date", MARCH_15, "total_ht", "100", "total_tva", "20",
                            "total_ttc", "120", "status", 1, "paye", 0, "totalpaid", "50", "remaintopay", "70")));
            data.put("invoices/40/payments", List.of(
                    Map.of("amount", "50", "type", "VIR", "typeline", "payment", "date", "2026-03-20 00:00:00", "num", "", "ref", "PAY2603-0001")));
            data.put("projects", List.of(
                    Map.of("id", 50, "ref", "PJ2603-0001", "title", "Nouveau four", "socid", 10, "status", 1, "date_start", MARCH_15)));
            // The sub-task comes before its parent, as can happen in Dolibarr
            data.put("tasks", List.of(
                    Map.of("id", 61, "ref", "TK-2", "label", "Commander les pièces", "fk_project", 50, "fk_task_parent", 60,
                            "progress", 100, "planned_workload", 7200),
                    Map.of("id", 60, "ref", "TK-1", "label", "Installer le four", "fk_project", 50, "fk_task_parent", 0,
                            "progress", 30)));
        }

        private static Map<String, Object> map(Object... kv) {
            Map<String, Object> m = new HashMap<>();
            for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
            return m;
        }

        @Override public void configure(String baseUrl, String apiKey) {}
        @Override public void clear() {}
        @Override public boolean testConnection() { return true; }
        @Override public String getBaseUrl() { return "https://fake-dolibarr"; }
        @Override public Map<String, Object> getOne(String resource, long id) { return null; }

        @Override
        public List<Map<String, Object>> getAll(String resource, Map<String, String> queryParams) {
            return getList(resource, queryParams);
        }

        @Override
        public List<Map<String, Object>> getList(String resource, Map<String, String> queryParams) {
            return data.getOrDefault(resource, List.of());
        }
    }

    @Autowired DolibarrImportOrchestrator orchestrator;
    @Autowired ThirdPartyRepository thirdPartyRepository;
    @Autowired ContactRepository contactRepository;
    @Autowired ContactThirdPartyLinkRepository linkRepository;
    @Autowired ProductRepository productRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProjectTaskRepository taskRepository;
    @Autowired TransactionTemplate tx;

    private long[] counts() {
        return new long[]{thirdPartyRepository.count(), contactRepository.count(), linkRepository.count(),
                productRepository.count(), invoiceRepository.count(), paymentRepository.count(),
                projectRepository.count(), taskRepository.count()};
    }

    @Test
    void importIsCompleteAndCanBeRunAgainWithoutDuplicates() {
        long[] before = counts();

        DolibarrImportRun first = orchestrator.runImport(TENANT, "https://fake-dolibarr", "key", null, null);
        assertThat(first.getStatus()).isEqualTo(DolibarrImportRun.STATUS_SUCCESS);

        long[] afterFirst = counts();
        // third party, contact, contact link, product, invoice, payment, project, 2 tasks
        long[] expectedNew = {1, 1, 1, 1, 1, 1, 1, 2};
        for (int i = 0; i < before.length; i++) {
            assertThat(afterFirst[i] - before[i]).as("new records, counter %d", i).isEqualTo(expectedNew[i]);
        }

        tx.executeWithoutResult(status -> {
            Invoice inv = invoiceRepository.findByReferenceAndTenantId("FA2603-0001", TENANT).orElseThrow();
            assertThat(inv.getDateIssued()).isEqualTo(LocalDate.of(2026, 3, 15));
            assertThat(inv.getAmountPaid()).isEqualByComparingTo("50");
            assertThat(inv.getAmountRemaining()).isEqualByComparingTo("70");
            assertThat(inv.getStatus()).isEqualTo("PARTIALLY_PAID");
            assertThat(inv.getType()).isEqualTo("SALES");
            assertThat(productRepository.findByCodeAndTenantIdAndDeletedAtIsNull("PAIN-01", TENANT).orElseThrow().getVatRate())
                    .as("5.5 % stored as a fraction").isEqualByComparingTo("0.055");
            assertThat(inv.getThirdParty().getCode()).isEqualTo("CU2601-0001");

            List<Payment> payments = paymentRepository.findByInvoiceIdOrderByPaymentDateAsc(inv.getId());
            assertThat(payments).singleElement().satisfies(p -> {
                assertThat(p.getAmount()).isEqualByComparingTo(new BigDecimal("50"));
                assertThat(p.getPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 20));
                assertThat(p.getReference()).isEqualTo("PAY2603-0001");
            });

            Project project = projectRepository.findByTenantIdAndCode(TENANT, "PJ2603-0001").orElseThrow();
            assertThat(project.getStatus()).isEqualTo("ACTIVE");
            assertThat(project.getDateStart()).isEqualTo(LocalDate.of(2026, 3, 15));
            assertThat(project.getThirdParty().getCode()).isEqualTo("CU2601-0001");

            List<ProjectTask> roots = taskRepository.findByProjectIdAndParentTaskIsNullOrderBySortOrderAsc(project.getId());
            assertThat(roots).singleElement().satisfies(root -> {
                assertThat(root.getTitle()).isEqualTo("Installer le four");
                assertThat(root.getStatus()).isEqualTo("IN_PROGRESS");
                assertThat(root.getSubTasks()).singleElement().satisfies(sub -> {
                    assertThat(sub.getTitle()).isEqualTo("Commander les pièces");
                    assertThat(sub.getStatus()).isEqualTo("DONE");
                    assertThat(sub.getEstimatedHours()).isEqualByComparingTo("2");
                });
            });
        });

        DolibarrImportRun second = orchestrator.runImport(TENANT, "https://fake-dolibarr", "key", null, null);
        assertThat(second.getStatus()).isEqualTo(DolibarrImportRun.STATUS_SUCCESS);
        assertThat(counts()).as("second run creates nothing").containsExactly(afterFirst);
    }

    @Test
    void timestampsAndDateStringsAreBothParsed() {
        assertThat(DolibarrImportHelper.getLocalDate(Map.of("d", MARCH_15), "d")).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(DolibarrImportHelper.getLocalDate(Map.of("d", "2026-03-20 00:00:00"), "d")).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(DolibarrImportHelper.getLocalDate(Map.of("d", ""), "d")).isNull();
        assertThat(DolibarrImportHelper.getLocalDate(Map.of("d", 0), "d")).isNull();
        assertThat(Instant.ofEpochSecond(MARCH_15)).isNotNull();
    }
}
