package fr.aplose.erp.core.web;

import fr.aplose.erp.tenant.plan.SubscriptionPlan;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    @GetMapping("/")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("plans", SubscriptionPlan.values());
        return "public/home";
    }

    @GetMapping("/tarifs")
    public String pricing(Model model) {
        model.addAttribute("plans", SubscriptionPlan.values());
        return "public/pricing";
    }
}
