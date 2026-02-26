package fr.aplose.erp.modules.publicform.web;

import fr.aplose.erp.modules.publicform.dto.FormFieldDto;
import fr.aplose.erp.modules.publicform.entity.PublicForm;
import fr.aplose.erp.modules.publicform.service.PublicFormService;
import fr.aplose.erp.tenant.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public (anonymous) controller for form display and submission.
 * Tenant is resolved by TenantResolutionFilter (subdomain or /t/{code}/).
 */
@Controller
@RequiredArgsConstructor
public class PublicFormController {

    private static final String SESSION_CAPTCHA = "publicFormCaptcha";

    private final PublicFormService publicFormService;

    @GetMapping(value = {"/form/{formSlug}", "/t/{tenantCode}/form/{formSlug}"})
    public String showForm(@PathVariable(required = false) String tenantCode,
                           @PathVariable String formSlug,
                           Model model,
                           HttpServletRequest request) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            model.addAttribute("messageKey", "publicform.no.tenant");
            return "public/form-error";
        }
        try {
            PublicForm form = publicFormService.findByTenantAndCodeForPublic(tenantId, formSlug);
            List<FormFieldDto> fields = publicFormService.getFormFields(form);
            model.addAttribute("form", form);
            model.addAttribute("fields", fields);
            String formAction = (tenantCode != null ? "/t/" + tenantCode + "/form/" + formSlug : "/form/" + formSlug);
            model.addAttribute("formAction", formAction);

            if (form.isCaptchaEnabled()) {
                SecureRandom r = new SecureRandom();
                int a = 1 + r.nextInt(9);
                int b = 1 + r.nextInt(9);
                request.getSession(true).setAttribute(SESSION_CAPTCHA, a + b);
                model.addAttribute("captchaA", a);
                model.addAttribute("captchaB", b);
            }
            return "public/form";
        } catch (IllegalArgumentException e) {
            model.addAttribute("messageKey", "publicform.not.found");
            return "public/form-error";
        }
    }

    @PostMapping(value = {"/form/{formSlug}", "/t/{tenantCode}/form/{formSlug}"})
    public String submitForm(@PathVariable(required = false) String tenantCode,
                             @PathVariable String formSlug,
                             HttpServletRequest request,
                             RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            ra.addFlashAttribute("messageKey", "publicform.no.tenant");
            return "redirect:/";
        }
        try {
            PublicForm form = publicFormService.findByTenantAndCodeForPublic(tenantId, formSlug);
            if (form.isCaptchaEnabled()) {
                Object expected = request.getSession(false) != null ? request.getSession().getAttribute(SESSION_CAPTCHA) : null;
                String answer = request.getParameter("captchaAnswer");
                if (expected == null || answer == null || !String.valueOf(expected).trim().equals(answer.trim())) {
                    ra.addFlashAttribute("errorKey", "publicform.captcha.invalid");
                    ra.addFlashAttribute("formSlug", formSlug);
                    return "redirect:" + (tenantCode != null ? "/t/" + tenantCode + "/form/" + formSlug : "/form/" + formSlug);
                }
                request.getSession().removeAttribute(SESSION_CAPTCHA);
            }
            Map<String, String> data = new HashMap<>();
            List<FormFieldDto> fields = publicFormService.getFormFields(form);
            for (FormFieldDto f : fields) {
                String val = request.getParameter(f.getName());
                if (val != null) data.put(f.getName(), val);
            }
            String ip = request.getRemoteAddr();
            publicFormService.submit(tenantId, formSlug, data, ip);
            ra.addFlashAttribute("successMessage", form.getSuccessMessage() != null && !form.getSuccessMessage().isBlank()
                    ? form.getSuccessMessage() : "publicform.thankyou");
            ra.addFlashAttribute("formName", form.getName());
            return "redirect:" + (tenantCode != null ? "/t/" + tenantCode + "/form/" + formSlug + "/thank-you" : "/form/" + formSlug + "/thank-you");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("messageKey", "publicform.not.found");
            return "redirect:/";
        }
    }

    @GetMapping(value = {"/form/{formSlug}/thank-you", "/t/{tenantCode}/form/{formSlug}/thank-you"})
    public String thankYou(@PathVariable(required = false) String tenantCode,
                           @PathVariable String formSlug,
                           Model model) {
        model.addAttribute("formSlug", formSlug);
        return "public/form-thankyou";
    }
}
