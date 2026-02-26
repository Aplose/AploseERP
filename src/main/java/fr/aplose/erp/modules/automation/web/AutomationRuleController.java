package fr.aplose.erp.modules.automation.web;

import fr.aplose.erp.modules.automation.entity.AutomationRule;
import fr.aplose.erp.modules.automation.service.AutomationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/automation/rules")
@PreAuthorize("hasAuthority('AUTOMATION_READ')")
@RequiredArgsConstructor
public class AutomationRuleController {

    private final AutomationRuleService ruleService;

    public static final List<String> TRIGGER_ENTITIES = List.of("INVOICE", "PROPOSAL", "SALES_ORDER");
    public static final List<String> TRIGGER_EVENTS = List.of("VALIDATED", "CREATED", "STATUS_CHANGED");
    public static final List<String> ACTION_TYPES = List.of("LOG", "SEND_EMAIL");

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rules", ruleService.findAll());
        return "modules/automation/rule-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('AUTOMATION_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("rule", new AutomationRule());
        model.addAttribute("triggerEntities", TRIGGER_ENTITIES);
        model.addAttribute("triggerEvents", TRIGGER_EVENTS);
        model.addAttribute("actionTypes", ACTION_TYPES);
        return "modules/automation/rule-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('AUTOMATION_CREATE')")
    public String create(@ModelAttribute AutomationRule rule, RedirectAttributes ra) {
        ruleService.save(rule);
        ra.addFlashAttribute("message", "automation.rule.saved");
        return "redirect:/automation/rules";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('AUTOMATION_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("rule", ruleService.findById(id));
        model.addAttribute("triggerEntities", TRIGGER_ENTITIES);
        model.addAttribute("triggerEvents", TRIGGER_EVENTS);
        model.addAttribute("actionTypes", ACTION_TYPES);
        return "modules/automation/rule-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('AUTOMATION_UPDATE')")
    public String update(@PathVariable Long id, @ModelAttribute AutomationRule rule, RedirectAttributes ra) {
        AutomationRule existing = ruleService.findById(id);
        rule.setId(existing.getId());
        rule.setTenantId(existing.getTenantId());
        rule.setCreatedAt(existing.getCreatedAt());
        ruleService.save(rule);
        ra.addFlashAttribute("message", "automation.rule.saved");
        return "redirect:/automation/rules";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('AUTOMATION_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        ruleService.deleteById(id);
        ra.addFlashAttribute("message", "automation.rule.deleted");
        return "redirect:/automation/rules";
    }
}
