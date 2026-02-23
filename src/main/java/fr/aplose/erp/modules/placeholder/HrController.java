package fr.aplose.erp.modules.placeholder;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hr")
public class HrController {

    @GetMapping
    @PreAuthorize("hasAuthority('HR_READ')")
    public String index(Model model) {
        model.addAttribute("moduleLabelKey", "module.hr");
        return "modules/module-placeholder";
    }
}
