package fr.aplose.erp.core.web;

import fr.aplose.erp.modules.agenda.service.AgendaEventService;
import fr.aplose.erp.modules.commerce.service.InvoiceService;
import fr.aplose.erp.modules.commerce.service.ProposalService;
import fr.aplose.erp.modules.project.service.ProjectService;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final MessageSource messageSource;
    private final InvoiceService invoiceService;
    private final ProposalService proposalService;
    private final ThirdPartyRepository thirdPartyRepo;
    private final ProjectService projectService;
    private final AgendaEventService agendaEventService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "moduleDisabled", required = false) Boolean moduleDisabled,
                            Model model) {
        if (Boolean.TRUE.equals(moduleDisabled)) {
            model.addAttribute("errorMessage", messageSource.getMessage("module.disabled.redirect", null, LocaleContextHolder.getLocale()));
        }
        String tid = TenantContext.getCurrentTenantId();

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("openInvoices", invoiceService.countOpen());
        kpi.put("openProposals", proposalService.countOpen());
        kpi.put("thirdParties", thirdPartyRepo.findByTenantIdAndDeletedAtIsNull(tid, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        kpi.put("activeProjects", projectService.countActive());
        model.addAttribute("kpi", kpi);

        model.addAttribute("overdueInvoices", invoiceService.findOverdue());
        model.addAttribute("upcomingEvents", agendaEventService.findUpcoming(7));

        return "modules/dashboard";
    }
}
