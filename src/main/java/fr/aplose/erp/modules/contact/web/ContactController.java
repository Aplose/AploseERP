package fr.aplose.erp.modules.contact.web;

import fr.aplose.erp.modules.contact.service.ContactService;
import fr.aplose.erp.modules.contact.web.dto.ContactDto;
import fr.aplose.erp.modules.thirdparty.service.ThirdPartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/contacts")
@PreAuthorize("hasAuthority('CONTACT_READ')")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService service;
    private final ThirdPartyService thirdPartyService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by("lastName", "firstName"));
        model.addAttribute("contacts", service.findAll(q, pageable));
        model.addAttribute("q", q);
        return "modules/contact/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("contact", service.findById(id));
        return "modules/contact/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public String newForm(@RequestParam(required = false) Long thirdPartyId, Model model) {
        var dto = new ContactDto();
        dto.setThirdPartyId(thirdPartyId);
        model.addAttribute("contact", dto);
        if (thirdPartyId != null) {
            model.addAttribute("linkedThirdParty", thirdPartyService.findById(thirdPartyId));
        }
        return "modules/contact/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public String create(@Valid @ModelAttribute("contact") ContactDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) return "modules/contact/form";
        var c = service.create(dto);
        ra.addFlashAttribute("successMessage", "Contact created");
        if (dto.getThirdPartyId() != null) {
            return "redirect:/third-parties/" + dto.getThirdPartyId();
        }
        return "redirect:/contacts/" + c.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('CONTACT_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var c = service.findById(id);
        var dto = new ContactDto();
        dto.setThirdPartyId(c.getThirdParty() != null ? c.getThirdParty().getId() : null);
        dto.setFirstName(c.getFirstName()); dto.setLastName(c.getLastName());
        dto.setJobTitle(c.getJobTitle()); dto.setDepartment(c.getDepartment());
        dto.setEmail(c.getEmail()); dto.setEmailSecondary(c.getEmailSecondary());
        dto.setPhone(c.getPhone()); dto.setPhoneMobile(c.getPhoneMobile()); dto.setFax(c.getFax());
        dto.setAddressLine1(c.getAddressLine1()); dto.setAddressLine2(c.getAddressLine2());
        dto.setCity(c.getCity()); dto.setStateProvince(c.getStateProvince());
        dto.setPostalCode(c.getPostalCode()); dto.setCountryCode(c.getCountryCode());
        dto.setNotes(c.getNotes()); dto.setPrimary(c.isPrimary()); dto.setStatus(c.getStatus());
        model.addAttribute("contact", dto);
        model.addAttribute("contactId", id);
        if (c.getThirdParty() != null) {
            model.addAttribute("linkedThirdParty", c.getThirdParty());
        }
        return "modules/contact/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('CONTACT_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("contact") ContactDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) { model.addAttribute("contactId", id); return "modules/contact/form"; }
        service.update(id, dto);
        ra.addFlashAttribute("successMessage", "Contact updated");
        return "redirect:/contacts/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('CONTACT_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        var c = service.findById(id);
        Long tpId = c.getThirdParty() != null ? c.getThirdParty().getId() : null;
        service.delete(id);
        ra.addFlashAttribute("successMessage", "Contact deleted");
        if (tpId != null) return "redirect:/third-parties/" + tpId;
        return "redirect:/contacts";
    }
}
