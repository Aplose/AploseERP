package fr.aplose.erp.modules.commerce.web;

import fr.aplose.erp.modules.commerce.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/follow-up")
@PreAuthorize("hasAuthority('FOLLOW_UP_READ')")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("overdueInvoices", followUpService.getOverdueInvoices());
        model.addAttribute("proposalsToFollowUp", followUpService.getProposalsToFollowUp());
        return "modules/commerce/follow-up";
    }
}
