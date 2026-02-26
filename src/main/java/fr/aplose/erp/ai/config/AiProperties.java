package fr.aplose.erp.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com";
    private String model = "gpt-4o-mini";

    /** Configured when enabled and base URL set. API key optional (e.g. Ollama). */
    public boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }

    public EffectiveAiConfig toEffectiveConfig() {
        return new EffectiveAiConfig(baseUrl, apiKey, model);
    }
}
