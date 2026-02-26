package fr.aplose.erp.ai.web;

import fr.aplose.erp.ai.service.AiService;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.modules.ticketing.entity.Ticket;
import fr.aplose.erp.modules.ticketing.service.TicketService;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoints for AI-assisted features (suggestion de libellés, ticket reply, conversion score, next action).
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final TicketService ticketService;
    private final ProposalRepository proposalRepository;
    private final ThirdPartyRepository thirdPartyRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("available", aiService.isAvailable()));
    }

    /**
     * Suggests a professional line description for quote/invoice from a short description.
     * POST body: { "description": "...", "productName": "..." (optional) }
     */
    @PostMapping("/suggest-line")
    @PreAuthorize("hasAuthority('AI_USE')")
    public ResponseEntity<Map<String, Object>> suggestLine(@RequestBody Map<String, String> body) {
        String description = body != null ? body.get("description") : null;
        String productName = body != null ? body.get("productName") : null;
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "suggestion", (String) null));
        }
        String suggestion = aiService.suggestLineDescription(description, productName);
        return ResponseEntity.ok(Map.of("available", true, "suggestion", suggestion != null ? suggestion : ""));
    }

    /**
     * Suggests a reply to a ticket. POST body: { "ticketId": 123 }
     */
    @PostMapping("/suggest-ticket-reply")
    @PreAuthorize("hasAuthority('AI_USE') and hasAuthority('TICKETING_UPDATE')")
    public ResponseEntity<Map<String, Object>> suggestTicketReply(@RequestBody Map<String, Object> body) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "suggestion", (String) null));
        }
        Long ticketId = body != null && body.get("ticketId") != null ? ((Number) body.get("ticketId")).longValue() : null;
        if (ticketId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "ticketId required"));
        }
        Ticket ticket = ticketService.findById(ticketId);
        String commentsText = ticket.getComments().stream()
                .map(c -> c.getContent() != null ? c.getContent() : "")
                .collect(Collectors.joining("\n"));
        String suggestion = aiService.suggestTicketReply(ticket.getSubject(), ticket.getDescription(), commentsText);
        return ResponseEntity.ok(Map.of("available", true, "suggestion", suggestion != null ? suggestion : ""));
    }

    /**
     * Suggests conversion score for a proposal. POST body: { "proposalId": 456 }
     */
    @PostMapping("/suggest-conversion-score")
    @PreAuthorize("hasAuthority('AI_USE')")
    public ResponseEntity<Map<String, Object>> suggestConversionScore(@RequestBody Map<String, Object> body) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "suggestion", (String) null));
        }
        Long proposalId = body != null && body.get("proposalId") != null ? ((Number) body.get("proposalId")).longValue() : null;
        if (proposalId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "proposalId required"));
        }
        String tid = TenantContext.getCurrentTenantId();
        var proposal = proposalRepository.findByIdAndTenantId(proposalId, tid)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));
        String ctx = "Référence: " + proposal.getReference() + ", Titre: " + (proposal.getTitle() != null ? proposal.getTitle() : "")
                + ", Montant TTC: " + proposal.getTotalAmount() + " " + proposal.getCurrencyCode()
                + ", Statut: " + proposal.getStatus()
                + ", Client: " + (proposal.getThirdParty() != null ? proposal.getThirdParty().getName() : "");
        String suggestion = aiService.suggestConversionScore(ctx);
        return ResponseEntity.ok(Map.of("available", true, "suggestion", suggestion != null ? suggestion : ""));
    }

    /**
     * Suggests next action for a third party. POST body: { "thirdPartyId": 789 }
     */
    @PostMapping("/suggest-next-action")
    @PreAuthorize("hasAuthority('AI_USE')")
    public ResponseEntity<Map<String, Object>> suggestNextAction(@RequestBody Map<String, Object> body) {
        if (!aiService.isAvailable()) {
            return ResponseEntity.ok(Map.of("available", false, "suggestion", (String) null));
        }
        Long tpId = body != null && body.get("thirdPartyId") != null ? ((Number) body.get("thirdPartyId")).longValue() : null;
        if (tpId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "thirdPartyId required"));
        }
        String tid = TenantContext.getCurrentTenantId();
        var tp = thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(tpId, tid)
                .orElseThrow(() -> new IllegalArgumentException("Third party not found"));
        var proposals = proposalRepository.findByTenantIdAndThirdPartyIdOrderByDateIssuedDesc(tid, tpId);
        var invoices = invoiceRepository.findByTenantIdAndThirdPartyIdOrderByDateIssuedDesc(tid, tpId);
        StringBuilder ctx = new StringBuilder();
        ctx.append("Nom: ").append(tp.getName()).append(", Code: ").append(tp.getCode());
        if (tp.isCustomer()) ctx.append(", Client");
        if (tp.isSupplier()) ctx.append(", Fournisseur");
        if (tp.isProspect()) ctx.append(", Prospect");
        ctx.append(". Nombre de devis: ").append(proposals.size()).append(", Nombre de factures: ").append(invoices.size());
        String suggestion = aiService.suggestNextActionForThirdParty(ctx.toString());
        return ResponseEntity.ok(Map.of("available", true, "suggestion", suggestion != null ? suggestion : ""));
    }
}
