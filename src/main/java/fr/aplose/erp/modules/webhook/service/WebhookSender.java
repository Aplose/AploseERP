package fr.aplose.erp.modules.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class WebhookSender {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String HMAC_SHA256 = "HmacSHA256";

    @Async
    public void sendAsync(String url, String secret, Map<String, Object> payload) {
        try {
            String body = OBJECT_MAPPER.writeValueAsString(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (secret != null && !secret.isBlank()) {
                headers.set(SIGNATURE_HEADER, "sha256=" + hmacHex(secret, body));
            }
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            RestTemplate rest = createRestTemplate();
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Webhook POST {} returned {}", url, response.getStatusCode());
            }
        } catch (JsonProcessingException e) {
            log.error("Webhook payload serialization error: {}", e.getMessage());
        } catch (ResourceAccessException e) {
            log.warn("Webhook delivery failed (timeout/connect): {} - {}", url, e.getMessage());
        } catch (Exception e) {
            log.warn("Webhook delivery failed: {} - {}", url, e.getMessage());
        }
    }

    private static String hmacHex(String secret, String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private static RestTemplate createRestTemplate() {
        RestTemplate rest = new RestTemplate();
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout(CONNECT_TIMEOUT_MS);
        f.setReadTimeout(READ_TIMEOUT_MS);
        rest.setRequestFactory(f);
        return rest;
    }
}
