package fr.aplose.erp.ai.web;

import fr.aplose.erp.ai.entity.AssistantAudit;
import fr.aplose.erp.ai.service.AssistantService;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @GetMapping
    @PreAuthorize("hasAuthority('AI_USE')")
    public String index(Model model, @AuthenticationPrincipal ErpUserDetails user,
                        @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("available", assistantService.isAvailable());
        Page<AssistantAudit> audit = assistantService.getAudit(user.getTenantId(), page, 10);
        model.addAttribute("auditPage", audit);
        return "modules/assistant/assistant";
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAuthority('AI_USE')")
    public String ask(@AuthenticationPrincipal ErpUserDetails user,
                     @RequestParam String question,
                     RedirectAttributes ra) {
        if (question == null || question.isBlank()) {
            ra.addFlashAttribute("error", "ai.assistant.questionRequired");
            return "redirect:/assistant";
        }
        String answer = assistantService.ask(user.getTenantId(), user.getUserId(), question.trim());
        ra.addFlashAttribute("lastQuestion", question.trim());
        ra.addFlashAttribute("lastAnswer", answer != null ? answer : "");
        return "redirect:/assistant";
    }
}
