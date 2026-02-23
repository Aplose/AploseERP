package fr.aplose.erp.mail.web;

import fr.aplose.erp.mail.entity.EmailTemplate;
import fr.aplose.erp.mail.service.EmailTemplateService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/email-templates")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'SUPER_ADMIN')")
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final MessageSource messageSource;

    public EmailTemplateController(EmailTemplateService emailTemplateService, MessageSource messageSource) {
        this.emailTemplateService = emailTemplateService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", emailTemplateService.findAll());
        return "modules/admin/email-templates-list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        EmailTemplate t = emailTemplateService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        model.addAttribute("template", t);
        return "modules/admin/email-template-form";
    }

    @PostMapping("/{id}/edit")
    public String save(@PathVariable Long id,
                      @Valid @ModelAttribute("template") EmailTemplate template,
                      BindingResult result,
                      Model model,
                      RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "modules/admin/email-template-form";
        }
        EmailTemplate existing = emailTemplateService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        existing.setSubject(template.getSubject());
        existing.setBodyHtml(template.getBodyHtml());
        existing.setBodyText(template.getBodyText());
        emailTemplateService.save(existing);
        ra.addFlashAttribute("successMessage", messageSource.getMessage("emailtemplate.saved", null, LocaleContextHolder.getLocale()));
        return "redirect:/admin/email-templates";
    }
}
