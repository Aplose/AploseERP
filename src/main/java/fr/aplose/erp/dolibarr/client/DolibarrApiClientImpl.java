package fr.aplose.erp.dolibarr.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DolibarrApiClientImpl implements DolibarrApiClient {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final String API_PATH = "/api/index.php/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private String baseUrl;
    private String apiKey;

    public DolibarrApiClientImpl() {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
        this.restTemplate.setRequestFactory(createRequestFactory());
    }

    private org.springframework.http.client.SimpleClientHttpRequestFactory createRequestFactory() {
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout(CONNECT_TIMEOUT_MS);
        f.setReadTimeout(READ_TIMEOUT_MS);
        return f;
    }

    /**
     * Configure client for a run (base URL and API key). Call before testConnection or getList/getOne.
     */
    public void configure(String baseUrl, String apiKey) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) return "";
        String u = url.trim();
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl != null ? baseUrl : "";
    }

    @Override
    public boolean testConnection() {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            return false;
        }
        try {
            List<Map<String, Object>> list = getList("thirdparties", Map.of("limit", "1", "sortfield", "t.rowid"));
            return list != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getList(String resource, Map<String, String> queryParams) {
        if (baseUrl == null || baseUrl.isBlank()) return Collections.emptyList();
        String url = baseUrl + API_PATH + resource;
        if (queryParams != null && !queryParams.isEmpty()) {
            String q = queryParams.entrySet().stream()
                    .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue() != null ? e.getValue() : "", java.nio.charset.StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            url = url + "?" + q;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", apiKey != null ? apiKey : "");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return Collections.emptyList();
            }
            String body = response.getBody();
            Object parsed = objectMapper.readValue(body, Object.class);
            if (parsed instanceof List) {
                return objectMapper.convertValue(parsed, new TypeReference<>() {});
            }
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) parsed;
                if (map.containsKey("error")) return Collections.emptyList();
                return List.of(map);
            }
            return Collections.emptyList();
        } catch (RestClientException e) {
            throw new DolibarrApiException("Dolibarr API call failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DolibarrApiException("Failed to parse Dolibarr response", e);
        }
    }

    @Override
    public Map<String, Object> getOne(String resource, long id) {
        if (baseUrl == null || baseUrl.isBlank()) return null;
        String url = baseUrl + API_PATH + resource + "/" + id;
        HttpHeaders headers = new HttpHeaders();
        headers.set("DOLAPIKEY", apiKey != null ? apiKey : "");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return null;
            }
            return objectMapper.readValue(response.getBody(), new TypeReference<>() {});
        } catch (RestClientException e) {
            throw new DolibarrApiException("Dolibarr API call failed: " + e.getMessage(), e);
        } catch (Exception e) {
            return null;
        }
    }
}
