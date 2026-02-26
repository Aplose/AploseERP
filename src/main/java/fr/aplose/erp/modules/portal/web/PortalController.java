package fr.aplose.erp.modules.portal.web;

import fr.aplose.erp.modules.ged.service.GedService;
import fr.aplose.erp.modules.portal.service.PortalService;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client portal: read-only access to proposals and invoices for the third party linked to the current user.
 * User must have PORTAL_ACCESS and third_party_id set.
 */
@Controller
@RequestMapping("/portal")
@PreAuthorize("hasAuthority('PORTAL_ACCESS')")
@RequiredArgsConstructor
public class PortalController {

    private static final String ENTITY_TYPE_THIRD_PARTY = "THIRD_PARTY";

    private final PortalService portalService;
    private final GedService gedService;

    @GetMapping
    public String index(@AuthenticationPrincipal ErpUserDetails principal, Model model) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) {
            return "redirect:/dashboard?portalNoThirdParty=1";
        }
        var proposals = portalService.getProposalsForThirdParty(principal.getTenantId(), thirdPartyId);
        var invoices = portalService.getInvoicesForThirdParty(principal.getTenantId(), thirdPartyId);
        var documents = portalService.getDocumentsForThirdParty(principal.getTenantId(), thirdPartyId);
        model.addAttribute("proposalsCount", proposals.size());
        model.addAttribute("invoicesCount", invoices.size());
        model.addAttribute("documentsCount", documents.size());
        return "modules/portal/dashboard";
    }

    @GetMapping("/proposals")
    public String proposals(@AuthenticationPrincipal ErpUserDetails principal, Model model) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) return "redirect:/portal";
        model.addAttribute("proposals", portalService.getProposalsForThirdParty(principal.getTenantId(), thirdPartyId));
        return "modules/portal/proposal-list";
    }

    @GetMapping("/proposals/{id}")
    public String proposalDetail(@AuthenticationPrincipal ErpUserDetails principal, @PathVariable Long id, Model model) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) return "redirect:/portal";
        return portalService.getProposalIfBelongsToThirdParty(id, principal.getTenantId(), thirdPartyId)
                .map(p -> {
                    model.addAttribute("proposal", p);
                    return "modules/portal/proposal-detail";
                })
                .orElse("redirect:/portal/proposals");
    }

    @GetMapping("/invoices")
    public String invoices(@AuthenticationPrincipal ErpUserDetails principal, Model model) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) return "redirect:/portal";
        model.addAttribute("invoices", portalService.getInvoicesForThirdParty(principal.getTenantId(), thirdPartyId));
        return "modules/portal/invoice-list";
    }

    @GetMapping("/invoices/{id}")
    public String invoiceDetail(@AuthenticationPrincipal ErpUserDetails principal, @PathVariable Long id, Model model) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) return "redirect:/portal";
        return portalService.getInvoiceIfBelongsToThirdParty(id, principal.getTenantId(), thirdPartyId)
                .map(inv -> {
                    model.addAttribute("invoice", inv);
                    return "modules/portal/invoice-detail";
                })
                .orElse("redirect:/portal/invoices");
    }

    @GetMapping("/documents")
    public String documents(@AuthenticationPrincipal ErpUserDetails principal, Model model) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) return "redirect:/portal";
        model.addAttribute("documents", portalService.getDocumentsForThirdParty(principal.getTenantId(), thirdPartyId));
        return "modules/portal/document-list";
    }

    @GetMapping("/documents/upload")
    public String uploadForm(@AuthenticationPrincipal ErpUserDetails principal) {
        if (portalService.getThirdPartyIdForUser(principal.getUserId()) == null) return "redirect:/portal";
        return "modules/portal/document-upload";
    }

    @PostMapping("/documents/upload")
    public String upload(@AuthenticationPrincipal ErpUserDetails principal,
                         @RequestParam("file") MultipartFile file,
                         RedirectAttributes redirectAttributes) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) return "redirect:/portal";
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "portal.upload.empty");
            return "redirect:/portal/documents/upload";
        }
        try {
            gedService.upload(ENTITY_TYPE_THIRD_PARTY, thirdPartyId, file, principal.getUserId());
            redirectAttributes.addFlashAttribute("message", "portal.upload.success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "portal.upload.failed");
        }
        return "redirect:/portal/documents";
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal ErpUserDetails principal, @PathVariable Long id) {
        Long thirdPartyId = portalService.getThirdPartyIdForUser(principal.getUserId());
        if (thirdPartyId == null) {
            return ResponseEntity.notFound().build();
        }
        return portalService.getDocumentIfBelongsToThirdParty(id, principal.getTenantId(), thirdPartyId)
                .map(doc -> {
                    try {
                        Resource resource = gedService.getFileResource(doc);
                        String filename = URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream"))
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"; filename*=UTF-8''" + filename)
                                .body(resource);
                    } catch (IOException e) {
                        return ResponseEntity.notFound().<Resource>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
