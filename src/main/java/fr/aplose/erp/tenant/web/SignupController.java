package fr.aplose.erp.tenant.web;

import fr.aplose.erp.i18n.service.ReferenceDataService;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SignupController {

    private final TenantRegistrationService tenantRegistrationService;
    private final ReferenceDataService referenceData;

    @GetMapping("/signup")
    public String form(@RequestParam(required = false) String plan, Model model) {
        SignupDto dto = new SignupDto();
        dto.setPlan(SubscriptionPlan.fromCode(plan).getCode());
        // Pre-select the visitor's language when the interface offers it
        String lang = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage();
        if (referenceData.isSupportedLanguage(lang)) dto.setDefaultLocale(lang);
        model.addAttribute("signup", dto);
        model.addAttribute("plans", SubscriptionPlan.values());
        return "public/signup";
    }

    /** Currency, language and time zone suggested for a country (used by the signup form). */
    @GetMapping(value = "/signup/defaults", produces = "application/json")
    @ResponseBody
    public ReferenceDataService.CountryDefaults defaults(@RequestParam String country) {
        return referenceData.defaultsFor(country);
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
        ra.addFlashAttribute("message", "signup.success");
        return "redirect:/login?registered";
    }
}
