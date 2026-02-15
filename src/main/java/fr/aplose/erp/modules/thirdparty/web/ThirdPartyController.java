package fr.aplose.erp.modules.thirdparty.web;

import fr.aplose.erp.modules.contact.service.ContactService;
import fr.aplose.erp.modules.thirdparty.service.ThirdPartyService;
import fr.aplose.erp.modules.thirdparty.web.dto.ThirdPartyDto;
import fr.aplose.erp.security.service.ErpUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/third-parties")
@PreAuthorize("hasAuthority('THIRD_PARTY_READ')")
@RequiredArgsConstructor
public class ThirdPartyController {

    private final ThirdPartyService service;
    private final ContactService contactService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String filter,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by("name"));
        model.addAttribute("thirdParties", service.findAll(q, filter, pageable));
        model.addAttribute("q", q);
        model.addAttribute("filter", filter);
        return "modules/thirdparty/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("tp", service.findById(id));
        model.addAttribute("contacts", contactService.findByThirdParty(id));
        return "modules/thirdparty/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('THIRD_PARTY_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("tp", new ThirdPartyDto());
        return "modules/thirdparty/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('THIRD_PARTY_CREATE')")
    public String create(@Valid @ModelAttribute("tp") ThirdPartyDto dto,
                         BindingResult result,
                         @AuthenticationPrincipal ErpUserDetails principal,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) return "modules/thirdparty/form";
        try {
            var tp = service.create(dto, principal.getUserId());
            ra.addFlashAttribute("successMessage", "Third party created");
            return "redirect:/third-parties/" + tp.getId();
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "modules/thirdparty/form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('THIRD_PARTY_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var tp = service.findById(id);
        var dto = new ThirdPartyDto();
        dto.setCode(tp.getCode()); dto.setName(tp.getName());
        dto.setCustomer(tp.isCustomer()); dto.setSupplier(tp.isSupplier()); dto.setProspect(tp.isProspect());
        dto.setLegalForm(tp.getLegalForm()); dto.setTaxId(tp.getTaxId());
        dto.setRegistrationNo(tp.getRegistrationNo()); dto.setWebsite(tp.getWebsite());
        dto.setPhone(tp.getPhone()); dto.setFax(tp.getFax()); dto.setEmail(tp.getEmail());
        dto.setAddressLine1(tp.getAddressLine1()); dto.setAddressLine2(tp.getAddressLine2());
        dto.setCity(tp.getCity()); dto.setStateProvince(tp.getStateProvince());
        dto.setPostalCode(tp.getPostalCode()); dto.setCountryCode(tp.getCountryCode());
        dto.setCurrencyCode(tp.getCurrencyCode()); dto.setPaymentTerms(tp.getPaymentTerms());
        dto.setCreditLimit(tp.getCreditLimit()); dto.setTags(tp.getTags());
        dto.setNotes(tp.getNotes()); dto.setStatus(tp.getStatus());
        model.addAttribute("tp", dto);
        model.addAttribute("tpId", id);
        return "modules/thirdparty/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('THIRD_PARTY_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("tp") ThirdPartyDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) { model.addAttribute("tpId", id); return "modules/thirdparty/form"; }
        try {
            service.update(id, dto);
            ra.addFlashAttribute("successMessage", "Third party updated");
            return "redirect:/third-parties/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("tpId", id);
            return "modules/thirdparty/form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('THIRD_PARTY_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("successMessage", "Third party deleted");
        return "redirect:/third-parties";
    }
}
