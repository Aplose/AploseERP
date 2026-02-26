package fr.aplose.erp.modules.thirdparty.web;

import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.modules.contact.entity.ContactThirdPartyLink;
import fr.aplose.erp.modules.contact.repository.ContactThirdPartyLinkRepository;
import fr.aplose.erp.modules.extrafield.entity.ExtraFieldDefinition;
import fr.aplose.erp.modules.extrafield.service.ExtraFieldService;
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/third-parties")
@PreAuthorize("hasAuthority('THIRD_PARTY_READ')")
@RequiredArgsConstructor
public class ThirdPartyController {

    private final ThirdPartyService service;
    private final ContactThirdPartyLinkRepository contactThirdPartyLinkRepository;
    private final DictionaryService dictionaryService;
    private final ExtraFieldService extraFieldService;

    private static final String ENTITY_TYPE_THIRD_PARTY = "THIRD_PARTY";

    private void addDictionaryModelAttributes(Model model) {
        model.addAttribute("countries", dictionaryService.findByType(DictionaryType.COUNTRY));
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        model.addAttribute("legalForms", dictionaryService.findByType(DictionaryType.LEGAL_FORM));
    }

    private void addExtraFieldModelAttributes(Model model, Long entityId) {
        List<ExtraFieldDefinition> definitions = extraFieldService.getActiveDefinitions(ENTITY_TYPE_THIRD_PARTY);
        model.addAttribute("extraFieldDefinitions", definitions);
        Map<String, String> values = entityId != null
                ? extraFieldService.getValues(ENTITY_TYPE_THIRD_PARTY, entityId)
                : new LinkedHashMap<>();
        model.addAttribute("extraFieldValues", values);
    }

    private Map<String, String> getExtraFieldValuesFromRequest(HttpServletRequest request) {
        List<ExtraFieldDefinition> defs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_THIRD_PARTY);
        Map<String, String> map = new LinkedHashMap<>();
        for (ExtraFieldDefinition def : defs) {
            if (!def.isVisibleOnForm()) continue;
            String paramName = "extraField_" + def.getFieldCode();
            String value = request.getParameter(paramName);
            if ("BOOLEAN".equals(def.getFieldType())) {
                map.put(def.getFieldCode(), "true".equals(value) ? "true" : "false");
            } else {
                map.put(def.getFieldCode(), value != null ? value : "");
            }
        }
        return map;
    }

    private void addExtraFieldModelAttributesFromRequest(Model model, HttpServletRequest request) {
        model.addAttribute("extraFieldDefinitions", extraFieldService.getActiveDefinitions(ENTITY_TYPE_THIRD_PARTY));
        model.addAttribute("extraFieldValues", getExtraFieldValuesFromRequest(request));
    }

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
        List<ContactThirdPartyLink> contactLinks = contactThirdPartyLinkRepository.findByThirdPartyId(id).stream()
                .filter(link -> link.getContact().getDeletedAt() == null)
                .toList();
        model.addAttribute("contactLinks", contactLinks);
        Map<String, String> linkTypeLabels = new LinkedHashMap<>();
        dictionaryService.findByType(DictionaryType.CONTACT_THIRD_PARTY_LINK_TYPE)
                .forEach(item -> linkTypeLabels.put(item.getCode(), item.getLabel()));
        model.addAttribute("linkTypeLabels", linkTypeLabels);
        List<ExtraFieldDefinition> detailDefs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_THIRD_PARTY).stream()
                .filter(ExtraFieldDefinition::isVisibleOnDetail)
                .toList();
        model.addAttribute("extraFieldDefinitionsDetail", detailDefs);
        model.addAttribute("extraFieldValues", extraFieldService.getValues(ENTITY_TYPE_THIRD_PARTY, id));
        return "modules/thirdparty/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('THIRD_PARTY_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("tp", new ThirdPartyDto());
        addDictionaryModelAttributes(model);
        addExtraFieldModelAttributes(model, null);
        return "modules/thirdparty/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('THIRD_PARTY_CREATE')")
    public String create(@Valid @ModelAttribute("tp") ThirdPartyDto dto,
                         BindingResult result,
                         @AuthenticationPrincipal ErpUserDetails principal,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            addDictionaryModelAttributes(model);
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/thirdparty/form";
        }
        try {
            var tp = service.create(dto, principal.getUserId());
            extraFieldService.saveValues(ENTITY_TYPE_THIRD_PARTY, tp.getId(), getExtraFieldValuesFromRequest(request));
            ra.addFlashAttribute("successMessage", "Third party created");
            return "redirect:/third-parties/" + tp.getId();
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addDictionaryModelAttributes(model);
            addExtraFieldModelAttributesFromRequest(model, request);
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
        addDictionaryModelAttributes(model);
        addExtraFieldModelAttributes(model, id);
        return "modules/thirdparty/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('THIRD_PARTY_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("tp") ThirdPartyDto dto,
                         BindingResult result,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("tpId", id);
            addDictionaryModelAttributes(model);
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/thirdparty/form";
        }
        try {
            service.update(id, dto);
            extraFieldService.saveValues(ENTITY_TYPE_THIRD_PARTY, id, getExtraFieldValuesFromRequest(request));
            ra.addFlashAttribute("successMessage", "Third party updated");
            return "redirect:/third-parties/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("tpId", id);
            addDictionaryModelAttributes(model);
            addExtraFieldModelAttributesFromRequest(model, request);
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
