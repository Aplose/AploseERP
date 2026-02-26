package fr.aplose.erp.ai.service;

import fr.aplose.erp.ai.config.AiProperties;
import fr.aplose.erp.ai.config.EffectiveAiConfig;
import fr.aplose.erp.ai.entity.TenantAiConfig;
import fr.aplose.erp.ai.provider.AiProvider;
import fr.aplose.erp.ai.repository.TenantAiConfigRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * High-level AI service: prompts paramétrables, config tenant (Ollama) ou globale.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private static final String SYSTEM_PROMPT_LINE = "Tu es un assistant pour un ERP. Tu réponds uniquement par du texte court et professionnel, sans préambule. Langue: français sauf si le contexte est en anglais.";

    private final AiProperties properties;
    private final AiProvider provider;
    private final TenantAiConfigRepository tenantAiConfigRepository;

    /** Resolve config: tenant override (e.g. Ollama URL) then global app.ai. */
    public EffectiveAiConfig getEffectiveConfig(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            Optional<TenantAiConfig> tenant = tenantAiConfigRepository.findByTenantId(tenantId);
            if (tenant.isPresent()) {
                EffectiveAiConfig c = tenant.get().toEffectiveConfig();
                if (c.isConfigured()) return c;
            }
        }
        return properties.isConfigured() ? properties.toEffectiveConfig() : null;
    }

    public boolean isAvailable() {
        String tid = TenantContext.getCurrentTenantId();
        EffectiveAiConfig config = getEffectiveConfig(tid);
        return config != null && config.isConfigured();
    }

    /**
     * Generic completion with optional context (map keys as {{key}} in prompts).
     */
    public String complete(String systemPrompt, String userMessage, Map<String, Object> context) {
        EffectiveAiConfig config = getEffectiveConfig(TenantContext.getCurrentTenantId());
        if (config == null || !config.isConfigured()) return null;
        return provider.completeWithConfig(config, systemPrompt, userMessage, context);
    }

    /**
     * Suggestion de libellé / ligne pour devis ou facture à partir d'une courte description.
     */
    public String suggestLineDescription(String shortDescription, String productNameOrNull) {
        EffectiveAiConfig config = getEffectiveConfig(TenantContext.getCurrentTenantId());
        if (config == null || !config.isConfigured()) return null;
        String user = "Propose un libellé de ligne pour un devis ou une facture, court et professionnel, à partir de la description suivante : « " + (shortDescription != null ? shortDescription : "") + " ».";
        if (productNameOrNull != null && !productNameOrNull.isBlank()) {
            user += " Produit ou service concerné : " + productNameOrNull + ".";
        }
        String result = provider.completeWithConfig(config, SYSTEM_PROMPT_LINE, user, Map.of());
        return result != null ? result.trim() : null;
    }

    private static final String SYSTEM_PROMPT_TICKET = "Tu es un assistant support. À partir du sujet, de la description et des commentaires d'un ticket, propose une courte réponse professionnelle et courtoise pour le client. Réponds uniquement par le texte de la réponse, sans préambule. Langue: français.";

    /** Résumé / suggestion de réponse pour un ticket (sujet + description + commentaires). */
    public String suggestTicketReply(String subject, String description, String commentsText) {
        EffectiveAiConfig config = getEffectiveConfig(TenantContext.getCurrentTenantId());
        if (config == null || !config.isConfigured()) return null;
        String context = "Sujet: " + (subject != null ? subject : "") + "\nDescription: " + (description != null ? description : "") + "\nCommentaires: " + (commentsText != null ? commentsText : "");
        String user = "Propose une réponse courte pour ce ticket:\n" + context;
        String result = provider.completeWithConfig(config, SYSTEM_PROMPT_TICKET, user, Map.of());
        return result != null ? result.trim() : null;
    }

    private static final String SYSTEM_PROMPT_SCORE = "Tu es un assistant commercial. Donne une estimation de la probabilité de conversion (de 0 à 100%) pour ce devis, avec une phrase courte justifiant. Format de réponse: uniquement un nombre entier (0-100) suivi d'un espace puis d'une courte phrase. Exemple: 65 Le client est engagé et le montant est cohérent. Langue: français.";

    /** Score de probabilité de conversion pour un devis (contexte: référence, titre, montant, statut, client). */
    public String suggestConversionScore(String proposalContext) {
        EffectiveAiConfig config = getEffectiveConfig(TenantContext.getCurrentTenantId());
        if (config == null || !config.isConfigured()) return null;
        String user = "Contexte du devis:\n" + (proposalContext != null ? proposalContext : "");
        String result = provider.completeWithConfig(config, SYSTEM_PROMPT_SCORE, user, Map.of());
        return result != null ? result.trim() : null;
    }

    private static final String SYSTEM_PROMPT_NEXT_ACTION = "Tu es un assistant commercial. À partir des informations sur un tiers (client/prospect), suggère la prochaine action commerciale à faire en une phrase courte et actionable. Langue: français. Réponds uniquement par cette phrase.";

    /** Prochaine action suggérée pour un tiers (contexte: nom, code, type, résumé activité). */
    public String suggestNextActionForThirdParty(String thirdPartyContext) {
        EffectiveAiConfig config = getEffectiveConfig(TenantContext.getCurrentTenantId());
        if (config == null || !config.isConfigured()) return null;
        String user = "Contexte du tiers:\n" + (thirdPartyContext != null ? thirdPartyContext : "");
        String result = provider.completeWithConfig(config, SYSTEM_PROMPT_NEXT_ACTION, user, Map.of());
        return result != null ? result.trim() : null;
    }

    public Optional<TenantAiConfig> getTenantConfig(String tenantId) {
        return tenantAiConfigRepository.findByTenantId(tenantId);
    }

    public TenantAiConfig saveTenantConfig(String tenantId, String baseUrl, String apiKey, String model) {
        TenantAiConfig c = tenantAiConfigRepository.findByTenantId(tenantId).orElse(new TenantAiConfig());
        c.setTenantId(tenantId);
        c.setBaseUrl(baseUrl != null ? baseUrl.trim() : "");
        c.setApiKey(apiKey != null && !apiKey.isBlank() ? apiKey.trim() : null);
        c.setModel(model != null && !model.isBlank() ? model.trim() : null);
        return tenantAiConfigRepository.save(c);
    }
}
