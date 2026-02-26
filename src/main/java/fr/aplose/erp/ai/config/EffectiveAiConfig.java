package fr.aplose.erp.ai.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resolved AI config for a request (tenant override or global).
 */
@Getter
@AllArgsConstructor
public class EffectiveAiConfig {
    private final String baseUrl;
    private final String apiKey;  // null for Ollama
    private final String model;

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
