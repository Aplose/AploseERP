package fr.aplose.erp.modules.placeholder;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reporting")
public class ReportingController {

    @GetMapping
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public String index(Model model) {
        model.addAttribute("moduleLabelKey", "module.reporting");
        return "modules/module-placeholder";
    }
}
