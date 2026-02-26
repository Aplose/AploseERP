package fr.aplose.erp.modules.publicform.web;

import fr.aplose.erp.modules.publicform.entity.PublicForm;
import fr.aplose.erp.modules.publicform.entity.PublicFormSubmission;
import fr.aplose.erp.modules.publicform.service.PublicFormService;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/public-forms")
@RequiredArgsConstructor
public class AdminPublicFormController {

    private final PublicFormService publicFormService;

    @GetMapping
    @PreAuthorize("hasAuthority('PUBLIC_FORM_READ')")
    public String list(@AuthenticationPrincipal ErpUserDetails user, Model model) {
        List<PublicForm> forms = publicFormService.findByTenant(user.getTenantId());
        model.addAttribute("forms", forms);
        return "modules/admin/public-forms-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PUBLIC_FORM_CREATE')")
    public String newForm(Model model) {
        PublicForm form = new PublicForm();
        form.setFieldsJson(defaultFieldsJson());
        form.setCaptchaEnabled(true);
        form.setEnabled(true);
        model.addAttribute("form", form);
        return "modules/admin/public-form-form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PUBLIC_FORM_UPDATE')")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails user, Model model) {
        PublicForm form = publicFormService.findByIdAndTenant(id, user.getTenantId());
        model.addAttribute("form", form);
        return "modules/admin/public-form-form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('PUBLIC_FORM_CREATE') or hasAuthority('PUBLIC_FORM_UPDATE')")
    public String save(@ModelAttribute PublicForm form,
                       @AuthenticationPrincipal ErpUserDetails user,
                       Model model,
                       RedirectAttributes ra) {
        try {
            publicFormService.save(form, user.getTenantId());
            ra.addFlashAttribute("message", "publicform.saved");
            return "redirect:/admin/public-forms";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            return "modules/admin/public-form-form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('PUBLIC_FORM_DELETE')")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails user, RedirectAttributes ra) {
        try {
            publicFormService.deleteById(id, user.getTenantId());
            ra.addFlashAttribute("message", "publicform.deleted");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "publicform.notfound");
        }
        return "redirect:/admin/public-forms";
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAuthority('PUBLIC_FORM_READ')")
    public String submissions(@PathVariable Long id,
                              @AuthenticationPrincipal ErpUserDetails user,
                              Pageable pageable,
                              Model model) {
        PublicForm form = publicFormService.findByIdAndTenant(id, user.getTenantId());
        Page<PublicFormSubmission> page = publicFormService.getSubmissionsByForm(id, user.getTenantId(), pageable);
        model.addAttribute("form", form);
        model.addAttribute("submissions", page);
        return "modules/admin/public-form-submissions";
    }

    private static String defaultFieldsJson() {
        return """
            [
              {"name":"name","label":"Nom","type":"text","required":true,"placeholder":""},
              {"name":"email","label":"Email","type":"email","required":true,"placeholder":""},
              {"name":"phone","label":"Téléphone","type":"tel","required":false,"placeholder":""},
              {"name":"message","label":"Message","type":"textarea","required":true,"placeholder":""}
            ]""";
    }
}
