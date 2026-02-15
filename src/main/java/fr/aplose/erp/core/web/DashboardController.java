package fr.aplose.erp.core.web;

import fr.aplose.erp.modules.commerce.service.InvoiceService;
import fr.aplose.erp.modules.commerce.service.ProposalService;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final InvoiceService invoiceService;
    private final ProposalService proposalService;
    private final ThirdPartyRepository thirdPartyRepo;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        String tid = TenantContext.getCurrentTenantId();

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("openInvoices", invoiceService.countOpen());
        kpi.put("openProposals", proposalService.countOpen());
        kpi.put("thirdParties", thirdPartyRepo.findByTenantIdAndDeletedAtIsNull(tid, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        kpi.put("activeProjects", 0L);
        model.addAttribute("kpi", kpi);

        model.addAttribute("overdueInvoices", invoiceService.findOverdue());

        return "modules/dashboard";
    }
}
