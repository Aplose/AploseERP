package fr.aplose.erp.modules.placeholder;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bank")
public class BankController {

    @GetMapping
    @PreAuthorize("hasAuthority('BANK_READ')")
    public String index(Model model) {
        model.addAttribute("moduleLabelKey", "module.bank");
        return "modules/module-placeholder";
    }
}
