package fr.aplose.erp.ai.provider;

import fr.aplose.erp.ai.config.EffectiveAiConfig;

import java.util.Map;

/**
 * Generic interface for calling an LLM (OpenAI, Azure OpenAI, Ollama, or compatible).
 */
public interface AiProvider {

    /**
     * Send a completion request with explicit config (tenant or global).
     *
     * @param config       Resolved config (baseUrl, optional apiKey, model).
     * @param systemPrompt Optional system message.
     * @param userMessage  User message; may contain {{key}} placeholders from context.
     * @param context      Optional map for placeholder substitution.
     * @return The model's text response, or null on failure.
     */
    String completeWithConfig(EffectiveAiConfig config, String systemPrompt, String userMessage, Map<String, Object> context);

    /**
     * Legacy: uses global AiProperties. Prefer completeWithConfig for tenant-aware calls.
     */
    default String complete(String systemPrompt, String userMessage, Map<String, Object> context) {
        return null;
    }
}
