package fr.aplose.erp.dolibarr.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dolibarr REST API client.
 * <p>
 * The bean is a singleton shared by all requests, so the connection settings (base URL, API key)
 * are kept per thread: two tenants importing at the same time never see each other's settings.
 * Callers must call {@link #clear()} when done.
 */
@Component
public class DolibarrApiClientImpl implements DolibarrApiClient {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String API_PATH = "/api/index.php/";

    /** Number of records requested per page by {@link #getAll}. */
    static final int PAGE_SIZE = 100;
    /** Safety net against an API that would ignore the "page" parameter and loop forever. */
    static final int MAX_PAGES = 10_000;

    private record Settings(String baseUrl, String apiKey) {}

    private final ThreadLocal<Settings> settings = new ThreadLocal<>();
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DolibarrApiClientImpl() {
        this(new RestTemplate(createRequestFactory()));
    }

    /** Visible for tests. */
    DolibarrApiClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(CONNECT_TIMEOUT_MS);
        f.setReadTimeout(READ_TIMEOUT_MS);
        return f;
    }

    @Override
    public void configure(String baseUrl, String apiKey) {
        settings.set(new Settings(normalizeBaseUrl(baseUrl), apiKey != null ? apiKey.trim() : ""));
    }

    @Override
    public void clear() {
        settings.remove();
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) return "";
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    @Override
    public String getBaseUrl() {
        Settings s = settings.get();
        return s != null ? s.baseUrl() : "";
    }

    private String apiKey() {
        Settings s = settings.get();
        return s != null ? s.apiKey() : "";
    }

    @Override
    public boolean testConnection() {
        if (getBaseUrl().isBlank() || apiKey().isBlank()) {
            return false;
        }
        // GET /status answers {"success": {"code": 200, "dolibarr_version": ...}} with a valid key.
        // Not a list call, so that a wrong URL (404) is not mistaken for an empty list.
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    getBaseUrl() + API_PATH + "status", HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return false;
            Map<String, Object> body = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            return body.containsKey("success");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getAll(String resource, Map<String, String> queryParams) {
        List<Map<String, Object>> all = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, String> params = new LinkedHashMap<>();
            if (queryParams != null) params.putAll(queryParams);
            params.put("limit", String.valueOf(PAGE_SIZE));
            params.put("page", String.valueOf(page));
            List<Map<String, Object>> chunk = getList(resource, params);
            all.addAll(chunk);
            if (chunk.size() < PAGE_SIZE) {
                return all;
            }
        }
        throw new DolibarrApiException("Pagination did not end for " + resource + " after " + MAX_PAGES + " pages");
    }

    @Override
    public List<Map<String, Object>> getList(String resource, Map<String, String> queryParams) {
        String baseUrl = getBaseUrl();
        if (baseUrl.isBlank()) return Collections.emptyList();
        String url = baseUrl + API_PATH + resource;
        if (queryParams != null && !queryParams.isEmpty()) {
            String q = queryParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue() != null ? e.getValue() : "", StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            url = url + "?" + q;
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return Collections.emptyList();
            }
            Object parsed = objectMapper.readValue(response.getBody(), Object.class);
            if (parsed instanceof List) {
                return objectMapper.convertValue(parsed, new TypeReference<>() {});
            }
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                if (map.containsKey("error")) return Collections.emptyList();
                // Response with pagination_data=true: {"data": [...], "pagination": {...}}
                if (map.get("data") instanceof List) {
                    return objectMapper.convertValue(map.get("data"), new TypeReference<>() {});
                }
                return List.of(map);
            }
            return Collections.emptyList();
        } catch (HttpClientErrorException e) {
            // Older Dolibarr versions answer 404 "No ... found" for an empty list (or a page past the end).
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            }
            throw new DolibarrApiException("Dolibarr API call failed on " + resource + ": " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new DolibarrApiException("Dolibarr API call failed on " + resource + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DolibarrApiException("Failed to parse Dolibarr response for " + resource, e);
        }
    }

    @Override
    public Map<String, Object> getOne(String resource, long id) {
        String baseUrl = getBaseUrl();
        if (baseUrl.isBlank()) return null;
        String url = baseUrl + API_PATH + resource + "/" + id;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers()), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) return null;
            throw new DolibarrApiException("Dolibarr API call failed: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new DolibarrApiException("Dolibarr API call failed: " + e.getMessage(), e);
        } catch (Exception e) {
            return null;
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", apiKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
