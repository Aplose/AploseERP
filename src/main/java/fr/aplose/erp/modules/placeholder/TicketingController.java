package fr.aplose.erp.modules.placeholder;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ticketing")
public class TicketingController {

    @GetMapping
    @PreAuthorize("hasAuthority('TICKETING_READ')")
    public String index(Model model) {
        model.addAttribute("moduleLabelKey", "module.ticketing");
        return "modules/module-placeholder";
    }
}
