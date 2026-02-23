package fr.aplose.erp.tenant.web;

import fr.aplose.erp.tenant.plan.SubscriptionPlan;
import fr.aplose.erp.tenant.service.TenantRegistrationService;
import fr.aplose.erp.tenant.web.dto.SignupDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SignupController {

    private static final String SESSION_TENANT_ID = "TENANT_ID";

    private final TenantRegistrationService tenantRegistrationService;

    @GetMapping("/signup")
    public String form(@RequestParam(required = false) String plan, Model model) {
        SignupDto dto = new SignupDto();
        dto.setPlan(SubscriptionPlan.fromCode(plan).getCode());
        model.addAttribute("signup", dto);
        model.addAttribute("plans", SubscriptionPlan.values());
        return "public/signup";
    }

    @PostMapping("/signup")
    public String submit(@Valid @ModelAttribute("signup") SignupDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra,
                         HttpServletRequest request) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "signup.passwordMismatch", "Les mots de passe ne correspondent pas.");
        }
        if (result.hasErrors()) {
            model.addAttribute("plans", SubscriptionPlan.values());
            return "public/signup";
        }
        String tenantId;
        try {
            tenantId = tenantRegistrationService.register(dto);
        } catch (Exception e) {
            result.reject("signup.failed", e.getMessage());
            model.addAttribute("plans", SubscriptionPlan.values());
            return "public/signup";
        }
        request.getSession(true).setAttribute(SESSION_TENANT_ID, tenantId);
        ra.addFlashAttribute("message", "signup.success");
        return "redirect:/login?registered";
    }
}
