package fr.aplose.erp.modules.commerce.web;

import fr.aplose.erp.modules.commerce.service.ProposalService;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.commerce.web.dto.ProposalDto;
import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.modules.extrafield.entity.ExtraFieldDefinition;
import fr.aplose.erp.modules.extrafield.service.ExtraFieldService;
import fr.aplose.erp.security.service.ErpUserDetails;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/proposals")
@PreAuthorize("hasAuthority('PROPOSAL_READ')")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService service;
    private final DictionaryService dictionaryService;
    private final ExtraFieldService extraFieldService;

    private static final String ENTITY_TYPE_PROPOSAL = "PROPOSAL";

    private void addExtraFieldModelAttributes(Model model, Long entityId) {
        List<ExtraFieldDefinition> definitions = extraFieldService.getActiveDefinitions(ENTITY_TYPE_PROPOSAL);
        model.addAttribute("extraFieldDefinitions", definitions);
        Map<String, String> values = entityId != null ? extraFieldService.getValues(ENTITY_TYPE_PROPOSAL, entityId) : new LinkedHashMap<>();
        model.addAttribute("extraFieldValues", values);
    }

    private Map<String, String> getExtraFieldValuesFromRequest(HttpServletRequest request) {
        List<ExtraFieldDefinition> defs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_PROPOSAL);
        Map<String, String> map = new LinkedHashMap<>();
        for (ExtraFieldDefinition def : defs) {
            if (!def.isVisibleOnForm()) continue;
            String paramName = "extraField_" + def.getFieldCode();
            String value = request.getParameter(paramName);
            if ("BOOLEAN".equals(def.getFieldType())) map.put(def.getFieldCode(), "true".equals(value) ? "true" : "false");
            else map.put(def.getFieldCode(), value != null ? value : "");
        }
        return map;
    }

    private void addExtraFieldModelAttributesFromRequest(Model model, HttpServletRequest request) {
        model.addAttribute("extraFieldDefinitions", extraFieldService.getActiveDefinitions(ENTITY_TYPE_PROPOSAL));
        model.addAttribute("extraFieldValues", getExtraFieldValuesFromRequest(request));
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "dateIssued"));
        model.addAttribute("proposals", service.findAll(q, status, pageable));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "modules/commerce/proposal-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var proposal = service.findById(id);
        model.addAttribute("proposal", proposal);
        model.addAttribute("newLine", new LineDto());
        List<ExtraFieldDefinition> detailDefs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_PROPOSAL).stream()
                .filter(ExtraFieldDefinition::isVisibleOnDetail).toList();
        model.addAttribute("extraFieldDefinitionsDetail", detailDefs);
        model.addAttribute("extraFieldValues", extraFieldService.getValues(ENTITY_TYPE_PROPOSAL, id));
        return "modules/commerce/proposal-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PROPOSAL_CREATE')")
    public String newForm(Model model) {
        var dto = new ProposalDto();
        dto.setDateIssued(java.time.LocalDate.now());
        dto.setDateValidUntil(java.time.LocalDate.now().plusDays(30));
        model.addAttribute("proposal", dto);
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        addExtraFieldModelAttributes(model, null);
        return "modules/commerce/proposal-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('PROPOSAL_CREATE')")
    public String create(@Valid @ModelAttribute("proposal") ProposalDto dto,
                         BindingResult result,
                         @AuthenticationPrincipal ErpUserDetails principal,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/commerce/proposal-form";
        }
        var p = service.create(dto, principal.getUserId());
        extraFieldService.saveValues(ENTITY_TYPE_PROPOSAL, p.getId(), getExtraFieldValuesFromRequest(request));
        ra.addFlashAttribute("successMessage", "Proposal " + p.getReference() + " created");
        return "redirect:/proposals/" + p.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PROPOSAL_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var p = service.findById(id);
        var dto = new ProposalDto();
        dto.setThirdPartyId(p.getThirdParty().getId());
        dto.setContactId(p.getContact() != null ? p.getContact().getId() : null);
        dto.setTitle(p.getTitle());
        dto.setDateIssued(p.getDateIssued());
        dto.setDateValidUntil(p.getDateValidUntil());
        dto.setCurrencyCode(p.getCurrencyCode());
        dto.setDiscountAmount(p.getDiscountAmount());
        dto.setNotes(p.getNotes());
        dto.setTerms(p.getTerms());
        dto.setSalesRepId(p.getSalesRep() != null ? p.getSalesRep().getId() : null);
        model.addAttribute("proposal", dto);
        model.addAttribute("proposalId", id);
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        addExtraFieldModelAttributes(model, id);
        return "modules/commerce/proposal-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PROPOSAL_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("proposal") ProposalDto dto,
                         BindingResult result,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("proposalId", id);
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/commerce/proposal-form";
        }
        try {
            service.update(id, dto);
            extraFieldService.saveValues(ENTITY_TYPE_PROPOSAL, id, getExtraFieldValuesFromRequest(request));
            ra.addFlashAttribute("successMessage", "Proposal updated");
            return "redirect:/proposals/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("proposalId", id);
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/commerce/proposal-form";
        }
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('PROPOSAL_UPDATE')")
    public String addLine(@PathVariable Long id,
                          @Valid @ModelAttribute("newLine") LineDto dto,
                          RedirectAttributes ra) {
        try {
            service.addLine(id, dto);
            ra.addFlashAttribute("successMessage", "Line added");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/proposals/" + id;
    }

    @PostMapping("/{id}/lines/{lineId}/delete")
    @PreAuthorize("hasAuthority('PROPOSAL_UPDATE')")
    public String removeLine(@PathVariable Long id, @PathVariable Long lineId, RedirectAttributes ra) {
        try {
            service.removeLine(id, lineId);
            ra.addFlashAttribute("successMessage", "Line removed");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/proposals/" + id;
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('PROPOSAL_UPDATE')")
    public String send(@PathVariable Long id, RedirectAttributes ra) {
        service.updateStatus(id, "SENT");
        ra.addFlashAttribute("successMessage", "Proposal marked as sent");
        return "redirect:/proposals/" + id;
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('PROPOSAL_UPDATE')")
    public String accept(@PathVariable Long id, RedirectAttributes ra) {
        service.updateStatus(id, "ACCEPTED");
        ra.addFlashAttribute("successMessage", "Proposal accepted");
        return "redirect:/proposals/" + id;
    }

    @PostMapping("/{id}/refuse")
    @PreAuthorize("hasAuthority('PROPOSAL_UPDATE')")
    public String refuse(@PathVariable Long id, RedirectAttributes ra) {
        service.updateStatus(id, "REFUSED");
        ra.addFlashAttribute("successMessage", "Proposal refused");
        return "redirect:/proposals/" + id;
    }
}
