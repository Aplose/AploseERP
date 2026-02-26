package fr.aplose.erp.modules.treasury.web;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.treasury.service.TreasuryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/treasury")
@PreAuthorize("hasAuthority('TREASURY_READ')")
@RequiredArgsConstructor
public class TreasuryController {

    private final TreasuryService treasuryService;

    @GetMapping
    public String index(Model model,
                        @RequestParam(defaultValue = "6") int months) {
        model.addAttribute("forecast", treasuryService.getForecastByMonth(months));
        model.addAttribute("months", Math.min(24, Math.max(1, months)));
        return "modules/treasury/dashboard";
    }

    @GetMapping("/inflows")
    public String inflows(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          Model model) {
        List<Invoice> list = treasuryService.getExpectedInflows(from, to);
        model.addAttribute("invoices", list);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("total", treasuryService.getTotalExpectedInflows(from, to));
        model.addAttribute("title", "Encaissements prévisionnels");
        return "modules/treasury/invoice-forecast-list";
    }

    @GetMapping("/outflows")
    public String outflows(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           Model model) {
        List<Invoice> list = treasuryService.getExpectedOutflows(from, to);
        model.addAttribute("invoices", list);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("total", treasuryService.getTotalExpectedOutflows(from, to));
        model.addAttribute("title", "Décaissements prévisionnels");
        return "modules/treasury/invoice-forecast-list";
    }
}
