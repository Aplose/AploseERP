package fr.aplose.erp.dolibarr.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DolibarrApiClientImplTest {

    private static final String BASE = "https://doli.example.com";

    private MockRestServiceServer server;
    private DolibarrApiClientImpl client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new DolibarrApiClientImpl(restTemplate);
        client.configure(BASE + "/", "secret-key");
    }

    @AfterEach
    void tearDown() {
        client.clear();
    }

    private static String page(int fromId, int count) {
        return IntStream.range(fromId, fromId + count)
                .mapToObj(i -> "{\"id\":" + i + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String url(String resource, int page) {
        return BASE + "/api/index.php/" + resource + "?sortfield=t.rowid&limit=100&page=" + page;
    }

    @Test
    void getAll_readsEveryPageUntilAnIncompleteOne() {
        server.expect(requestTo(url("thirdparties", 0))).andExpect(header("DOLAPIKEY", "secret-key"))
                .andRespond(withSuccess(page(1, 100), MediaType.APPLICATION_JSON));
        server.expect(requestTo(url("thirdparties", 1)))
                .andRespond(withSuccess(page(101, 100), MediaType.APPLICATION_JSON));
        server.expect(requestTo(url("thirdparties", 2)))
                .andRespond(withSuccess(page(201, 30), MediaType.APPLICATION_JSON));

        List<Map<String, Object>> all = client.getAll("thirdparties", Map.of("sortfield", "t.rowid"));

        assertThat(all).hasSize(230);
        assertThat(all.get(229).get("id")).isEqualTo(230);
        server.verify();
    }

    @Test
    void getAll_stopsOn404AfterAFullPage() {
        // Older Dolibarr versions answer 404 "No ... found" for a page past the end
        server.expect(requestTo(url("invoices", 0)))
                .andRespond(withSuccess(page(1, 100), MediaType.APPLICATION_JSON));
        server.expect(requestTo(url("invoices", 1)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"error\":{\"code\":404,\"message\":\"Not Found: No invoice found\"}}"));

        assertThat(client.getAll("invoices", Map.of("sortfield", "t.rowid"))).hasSize(100);
        server.verify();
    }

    @Test
    void getAll_emptyResourceGivesEmptyList() {
        server.expect(requestTo(url("projects", 0)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.getAll("projects", Map.of("sortfield", "t.rowid"))).isEmpty();
    }

    @Test
    void getList_authenticationErrorIsReported() {
        server.expect(requestTo(BASE + "/api/index.php/thirdparties"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getList("thirdparties", Map.of()))
                .isInstanceOf(DolibarrApiException.class);
    }

    @Test
    void testConnection_checksTheStatusEndpoint() {
        server.expect(requestTo(BASE + "/api/index.php/status"))
                .andRespond(withSuccess("{\"success\":{\"code\":200,\"dolibarr_version\":\"21.0.0\"}}", MediaType.APPLICATION_JSON));
        assertThat(client.testConnection()).isTrue();

        server.reset();
        server.expect(requestTo(BASE + "/api/index.php/status")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(client.testConnection()).isFalse();
    }

    @Test
    void settingsAreKeptPerThread() throws Exception {
        AtomicReference<String> seenByOtherThread = new AtomicReference<>();
        Thread other = new Thread(() -> {
            client.configure("https://other-tenant.example.com", "other-key");
            seenByOtherThread.set(client.getBaseUrl());
            client.clear();
        });
        other.start();
        other.join();

        assertThat(seenByOtherThread.get()).isEqualTo("https://other-tenant.example.com");
        assertThat(client.getBaseUrl()).isEqualTo(BASE);
    }
}
