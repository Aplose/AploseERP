package fr.aplose.erp.modules.crm.web;

import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.crm.entity.CrmActivity;
import fr.aplose.erp.modules.crm.service.CrmActivityService;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/crm/activities")
@RequiredArgsConstructor
public class CrmActivityController {

    private final CrmActivityService activityService;
    private final ThirdPartyRepository thirdPartyRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_READ')")
    public String list(@RequestParam(required = false) Long thirdPartyId, Model model) {
        List<CrmActivity> activities = thirdPartyId != null
                ? activityService.findByThirdParty(thirdPartyId)
                : activityService.findAll();
        model.addAttribute("activities", activities);
        model.addAttribute("thirdPartyId", thirdPartyId);
        if (thirdPartyId != null) {
            thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId())
                    .ifPresent(tp -> model.addAttribute("thirdParty", tp));
        }
        return "modules/crm/activity-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_CREATE')")
    public String newForm(@RequestParam(required = false) Long thirdPartyId, Model model) {
        CrmActivity activity = new CrmActivity();
        if (thirdPartyId != null) {
            thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId())
                    .ifPresent(activity::setThirdParty);
        }
        model.addAttribute("activity", activity);
        addFormRefs(model);
        return "modules/crm/activity-form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_CREATE')")
    public String create(@ModelAttribute @Valid CrmActivity activity,
                         @RequestParam(required = false) Long thirdPartyId,
                         @RequestParam(required = false) Long contactId,
                         @RequestParam(required = false) Long assignedToId,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (thirdPartyId == null) result.rejectValue("thirdParty", "required", "Third party is required");
        if (result.hasErrors()) {
            addFormRefs(model);
            return "modules/crm/activity-form";
        }
        thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId())
                .ifPresent(activity::setThirdParty);
        if (contactId != null) contactRepository.findByIdAndTenantIdAndDeletedAtIsNull(contactId, TenantContext.getCurrentTenantId()).ifPresent(activity::setContact);
        if (assignedToId != null) userRepository.findByIdAndTenantId(assignedToId, TenantContext.getCurrentTenantId()).ifPresent(activity::setAssignedTo);
        activityService.save(activity);
        ra.addFlashAttribute("message", "Activity created.");
        return "redirect:/crm/activities" + (thirdPartyId != null ? "?thirdPartyId=" + thirdPartyId : "");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_READ')")
    public String detail(@PathVariable Long id, Model model) {
        return activityService.findById(id)
                .map(a -> {
                    model.addAttribute("activity", a);
                    return "modules/crm/activity-detail";
                })
                .orElse("redirect:/crm/activities");
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        return activityService.findById(id)
                .map(a -> {
                    model.addAttribute("activity", a);
                    addFormRefs(model);
                    return "modules/crm/activity-form";
                })
                .orElse("redirect:/crm/activities");
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_UPDATE')")
    public String update(@PathVariable Long id,
                         @ModelAttribute @Valid CrmActivity activity,
                         @RequestParam(required = false) Long thirdPartyId,
                         @RequestParam(required = false) Long contactId,
                         @RequestParam(required = false) Long assignedToId,
                         BindingResult result, Model model, RedirectAttributes ra) {
        CrmActivity existing = activityService.findById(id).orElse(null);
        if (existing == null) return "redirect:/crm/activities";
        if (result.hasErrors()) {
            activity.setId(id);
            model.addAttribute("activity", activity);
            addFormRefs(model);
            return "modules/crm/activity-form";
        }
        activity.setId(id);
        activity.setTenantId(existing.getTenantId());
        activity.setCreatedAt(existing.getCreatedAt());
        if (thirdPartyId != null) thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(thirdPartyId, TenantContext.getCurrentTenantId()).ifPresent(activity::setThirdParty);
        if (contactId != null) contactRepository.findByIdAndTenantIdAndDeletedAtIsNull(contactId, TenantContext.getCurrentTenantId()).ifPresent(activity::setContact);
        else activity.setContact(null);
        if (assignedToId != null) userRepository.findByIdAndTenantId(assignedToId, TenantContext.getCurrentTenantId()).ifPresent(activity::setAssignedTo);
        else activity.setAssignedTo(null);
        activityService.save(activity);
        ra.addFlashAttribute("message", "Activity updated.");
        return "redirect:/crm/activities/" + id;
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_UPDATE')")
    public String toggleComplete(@PathVariable Long id, @RequestParam boolean completed, RedirectAttributes ra) {
        activityService.setCompleted(id, completed);
        ra.addFlashAttribute("message", completed ? "Activity marked done." : "Activity reopened.");
        return "redirect:/crm/activities/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('CRM_ACTIVITY_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        CrmActivity a = activityService.findById(id).orElse(null);
        Long tpId = a != null && a.getThirdParty() != null ? a.getThirdParty().getId() : null;
        activityService.delete(id);
        ra.addFlashAttribute("message", "Activity deleted.");
        return "redirect:/crm/activities" + (tpId != null ? "?thirdPartyId=" + tpId : "");
    }

    private void addFormRefs(Model model) {
        String tid = TenantContext.getCurrentTenantId();
        model.addAttribute("thirdParties", thirdPartyRepository.findByTenantIdAndDeletedAtIsNull(tid, PageRequest.of(0, 500, Sort.by("name"))).getContent());
        model.addAttribute("contacts", contactRepository.findByTenantIdAndDeletedAtIsNull(tid, PageRequest.of(0, 500, Sort.by("lastName"))).getContent());
        model.addAttribute("users", userRepository.findByTenantIdAndDeletedAtIsNull(tid, PageRequest.of(0, 200, Sort.by("lastName"))).getContent());
    }
}
