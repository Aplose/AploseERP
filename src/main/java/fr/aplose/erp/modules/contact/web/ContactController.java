package fr.aplose.erp.modules.contact.web;

import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.contact.entity.ContactThirdPartyLink;
import fr.aplose.erp.modules.contact.service.ContactService;
import fr.aplose.erp.modules.contact.web.dto.ContactDto;
import fr.aplose.erp.modules.contact.web.dto.ContactThirdPartyLinkDto;
import fr.aplose.erp.modules.extrafield.entity.ExtraFieldDefinition;
import fr.aplose.erp.modules.extrafield.service.ExtraFieldService;
import fr.aplose.erp.modules.thirdparty.service.ThirdPartyService;
import jakarta.servlet.http.HttpServletRequest;
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

import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/contacts")
@PreAuthorize("hasAuthority('CONTACT_READ')")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService service;
    private final ThirdPartyService thirdPartyService;
    private final DictionaryService dictionaryService;
    private final ExtraFieldService extraFieldService;

    private static final String ENTITY_TYPE_CONTACT = "CONTACT";

    private void addDictionaryModelAttributes(Model model) {
        model.addAttribute("civilities", dictionaryService.findByType(DictionaryType.CIVILITY));
        model.addAttribute("countries", dictionaryService.findByType(DictionaryType.COUNTRY));
        model.addAttribute("linkTypes", dictionaryService.findByType(DictionaryType.CONTACT_THIRD_PARTY_LINK_TYPE));
    }

    private void addExtraFieldModelAttributes(Model model, Long entityId) {
        List<ExtraFieldDefinition> definitions = extraFieldService.getActiveDefinitions(ENTITY_TYPE_CONTACT);
        model.addAttribute("extraFieldDefinitions", definitions);
        Map<String, String> values = entityId != null
                ? extraFieldService.getValues(ENTITY_TYPE_CONTACT, entityId)
                : new LinkedHashMap<>();
        model.addAttribute("extraFieldValues", values);
    }

    private Map<String, String> getExtraFieldValuesFromRequest(HttpServletRequest request) {
        List<ExtraFieldDefinition> defs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_CONTACT);
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
        model.addAttribute("extraFieldDefinitions", extraFieldService.getActiveDefinitions(ENTITY_TYPE_CONTACT));
        model.addAttribute("extraFieldValues", getExtraFieldValuesFromRequest(request));
    }

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
        List<ExtraFieldDefinition> detailDefs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_CONTACT).stream()
                .filter(ExtraFieldDefinition::isVisibleOnDetail)
                .toList();
        model.addAttribute("extraFieldDefinitionsDetail", detailDefs);
        model.addAttribute("extraFieldValues", extraFieldService.getValues(ENTITY_TYPE_CONTACT, id));
        Map<String, String> linkTypeLabels = new LinkedHashMap<>();
        dictionaryService.findByType(DictionaryType.CONTACT_THIRD_PARTY_LINK_TYPE)
                .forEach(item -> linkTypeLabels.put(item.getCode(), item.getLabel()));
        model.addAttribute("linkTypeLabels", linkTypeLabels);
        return "modules/contact/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public String newForm(@RequestParam(required = false) Long thirdPartyId, Model model) {
        var dto = new ContactDto();
        if (thirdPartyId != null) {
            var link = new ContactThirdPartyLinkDto();
            link.setThirdPartyId(thirdPartyId);
            link.setLinkTypeCode("SALARIE");
            dto.getLinks().add(link);
        }
        ensureAtLeastOneEmptyLink(dto.getLinks());
        model.addAttribute("contact", dto);
        addDictionaryModelAttributes(model);
        addExtraFieldModelAttributes(model, null);
        model.addAttribute("thirdParties", thirdPartyService.findAll("", null, PageRequest.of(0, 5000, Sort.by("name"))).getContent());
        if (thirdPartyId != null) {
            ThirdParty firstTp = thirdPartyService.findById(thirdPartyId);
            if (firstTp != null) model.addAttribute("firstLinkedThirdParty", firstTp);
        }
        return "modules/contact/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('CONTACT_CREATE')")
    public String create(@Valid @ModelAttribute("contact") ContactDto dto,
                         BindingResult result,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            addDictionaryModelAttributes(model);
            addExtraFieldModelAttributesFromRequest(model, request);
            model.addAttribute("thirdParties", thirdPartyService.findAll("", null, PageRequest.of(0, 5000, Sort.by("name"))).getContent());
            return "modules/contact/form";
        }
        var c = service.create(dto);
        extraFieldService.saveValues(ENTITY_TYPE_CONTACT, c.getId(), getExtraFieldValuesFromRequest(request));
        ra.addFlashAttribute("successMessage", "Contact created");
        if (dto.getLinks() != null && !dto.getLinks().isEmpty() && dto.getLinks().get(0).getThirdPartyId() != null) {
            return "redirect:/third-parties/" + dto.getLinks().get(0).getThirdPartyId();
        }
        return "redirect:/contacts/" + c.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('CONTACT_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var c = service.findById(id);
        var dto = new ContactDto();
        List<ContactThirdPartyLinkDto> links = new ArrayList<>();
        for (ContactThirdPartyLink link : c.getThirdPartyLinks()) {
            var linkDto = new ContactThirdPartyLinkDto();
            linkDto.setThirdPartyId(link.getThirdParty().getId());
            linkDto.setLinkTypeCode(link.getLinkTypeCode());
            links.add(linkDto);
        }
        ensureAtLeastOneEmptyLink(links);
        dto.setLinks(links);
        dto.setCivility(c.getCivility());
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
        addDictionaryModelAttributes(model);
        addExtraFieldModelAttributes(model, id);
        model.addAttribute("thirdParties", thirdPartyService.findAll("", null, PageRequest.of(0, 5000, Sort.by("name"))).getContent());
        if (!links.isEmpty() && links.get(0).getThirdPartyId() != null) {
            ThirdParty firstTp = thirdPartyService.findById(links.get(0).getThirdPartyId());
            if (firstTp != null) model.addAttribute("firstLinkedThirdParty", firstTp);
        }
        return "modules/contact/form";
    }

    private void ensureAtLeastOneEmptyLink(List<ContactThirdPartyLinkDto> links) {
        if (links == null) return;
        if (links.isEmpty() || links.get(links.size() - 1).getThirdPartyId() != null) {
            links.add(new ContactThirdPartyLinkDto());
        }
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('CONTACT_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("contact") ContactDto dto,
                         BindingResult result,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("contactId", id);
            addDictionaryModelAttributes(model);
            addExtraFieldModelAttributesFromRequest(model, request);
            model.addAttribute("thirdParties", thirdPartyService.findAll("", null, PageRequest.of(0, 5000, Sort.by("name"))).getContent());
            return "modules/contact/form";
        }
        service.update(id, dto);
        extraFieldService.saveValues(ENTITY_TYPE_CONTACT, id, getExtraFieldValuesFromRequest(request));
        ra.addFlashAttribute("successMessage", "Contact updated");
        return "redirect:/contacts/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('CONTACT_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        var c = service.findById(id);
        Long tpId = c.getThirdPartyLinks().isEmpty() ? null : c.getThirdPartyLinks().get(0).getThirdParty().getId();
        service.delete(id);
        ra.addFlashAttribute("successMessage", "Contact deleted");
        if (tpId != null) return "redirect:/third-parties/" + tpId;
        return "redirect:/contacts";
    }
}
