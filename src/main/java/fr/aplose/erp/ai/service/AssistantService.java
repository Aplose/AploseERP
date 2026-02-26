package fr.aplose.erp.ai.service;

import fr.aplose.erp.ai.entity.AssistantAudit;
import fr.aplose.erp.ai.repository.AssistantAuditRepository;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.entity.Tenant;
import fr.aplose.erp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Assistant / chatbot : questions en langage naturel sur les données du tenant.
 * Contexte métier injecté dans le prompt, audit des requêtes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private static final String SYSTEM_PROMPT = "Tu es un assistant pour un logiciel ERP. Tu réponds de façon concise et factuelle en français. "
            + "Tu ne donnes que des réponses basées sur le contexte fourni ou des généralités sur l'utilisation d'un ERP. "
            + "Si la question dépasse le contexte, dis que tu n'as pas l'information.";

    private final AiService aiService;
    private final AssistantAuditRepository auditRepository;
    private final TenantRepository tenantRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProposalRepository proposalRepository;

    public boolean isAvailable() {
        return aiService.isAvailable();
    }

    /**
     * Ask the assistant a question. Context is built from tenant data (counts, recent refs).
     */
    @Transactional
    public String ask(String tenantId, Long userId, String question) {
        if (question == null || question.isBlank()) return null;
        String context = buildContext(tenantId);
        String userMessage = "Contexte ERP du tenant (données actuelles):\n" + context + "\n\nQuestion: " + question;
        String answer = aiService.complete(SYSTEM_PROMPT, userMessage, Map.of());
        if (answer == null) answer = "";

        AssistantAudit audit = new AssistantAudit();
        audit.setTenantId(tenantId);
        audit.setUserId(userId);
        audit.setQuestion(question);
        audit.setAnswerPreview(answer.length() > 500 ? answer.substring(0, 500) + "…" : answer);
        auditRepository.save(audit);

        return answer;
    }

    public Page<AssistantAudit> getAudit(String tenantId, int page, int size) {
        return auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size));
    }

    private String buildContext(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return "(aucun tenant)";
        StringBuilder sb = new StringBuilder();
        tenantRepository.findById(tenantId).map(Tenant::getName).ifPresent(n -> sb.append("Organisation: ").append(n).append("\n"));
        long thirdParties = thirdPartyRepository.findByTenantIdAndDeletedAtIsNull(tenantId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        sb.append("Nombre de tiers: ").append(thirdParties).append("\n");
        long openInvoices = invoiceRepository.countByTenantIdAndStatusIn(tenantId, Set.of("DRAFT", "VALIDATED", "SENT", "PARTIALLY_PAID"));
        sb.append("Factures en cours (non soldées): ").append(openInvoices).append("\n");
        long openProposals = proposalRepository.countByTenantIdAndStatusIn(tenantId, Set.of("DRAFT", "SENT"));
        sb.append("Devis en attente: ").append(openProposals).append("\n");
        return sb.toString();
    }
}
