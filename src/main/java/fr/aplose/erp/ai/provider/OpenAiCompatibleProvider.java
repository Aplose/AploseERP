package fr.aplose.erp.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.aplose.erp.ai.config.AiProperties;
import fr.aplose.erp.ai.config.EffectiveAiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calls an OpenAI-compatible chat completions API (OpenAI, Azure OpenAI, or Ollama).
 * Ollama: base_url e.g. http://localhost:11434, no api_key. Azure: full deployment URL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiCompatibleProvider implements AiProvider {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final AiProperties properties;

    @Override
    public String completeWithConfig(EffectiveAiConfig config, String systemPrompt, String userMessage, Map<String, Object> context) {
        if (config == null || !config.isConfigured()) return null;
        String resolvedUser = resolvePlaceholders(userMessage, context);
        String url = buildUrl(config.getBaseUrl());
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", resolvePlaceholders(systemPrompt, context)));
        }
        messages.add(Map.of("role", "user", "content", resolvedUser));

        String modelName = config.getModel() != null && !config.getModel().isBlank() ? config.getModel() : DEFAULT_MODEL;
        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", messages,
                "max_tokens", 1024,
                "temperature", 0.3
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                headers.setBearerAuth(config.getApiKey().trim());
            }
            HttpEntity<String> entity = new HttpEntity<>(OBJECT_MAPPER.writeValueAsString(body), headers);
            RestTemplate rest = createRestTemplate();
            ResponseEntity<String> response = rest.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("AI API returned {}: {}", response.getStatusCode(), response.getBody());
                return null;
            }
            JsonNode root = OBJECT_MAPPER.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) return null;
            return choices.get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("AI completion failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String complete(String systemPrompt, String userMessage, Map<String, Object> context) {
        if (!properties.isConfigured()) return null;
        return completeWithConfig(properties.toEffectiveConfig(), systemPrompt, userMessage, context);
    }

    private static String buildUrl(String base) {
        if (base == null || base.isBlank()) return null;
        String b = base.trim();
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.contains("/chat/completions") || b.contains("/openai/deployments")) return b;
        return b + "/v1/chat/completions";
    }

    private static String resolvePlaceholders(String template, Map<String, Object> context) {
        if (template == null) return "";
        if (context == null || context.isEmpty()) return template;
        String out = template;
        for (Map.Entry<String, Object> e : context.entrySet()) {
            String placeholder = "{{" + e.getKey() + "}}";
            String value = e.getValue() != null ? e.getValue().toString() : "";
            out = out.replace(placeholder, value);
        }
        return out;
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
