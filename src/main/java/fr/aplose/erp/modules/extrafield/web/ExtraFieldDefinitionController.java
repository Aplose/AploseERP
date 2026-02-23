package fr.aplose.erp.modules.extrafield.web;

import fr.aplose.erp.modules.extrafield.entity.ExtraFieldDefinition;
import fr.aplose.erp.modules.extrafield.service.EntityFieldRegistry;
import fr.aplose.erp.modules.extrafield.service.ExtraFieldService;
import fr.aplose.erp.modules.extrafield.web.dto.ExtraFieldDefinitionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/extrafields")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('EXTRAFIELD_ADMIN')")
public class ExtraFieldDefinitionController {

    private final ExtraFieldService extraFieldService;

    @GetMapping
    public String listTypes(Model model) {
        model.addAttribute("entityTypes", EntityFieldRegistry.ENTITY_TYPES);
        return "modules/admin/extrafield-types";
    }

    @GetMapping("/{entityType}")
    public String listDefinitions(@PathVariable String entityType, Model model) {
        if (!EntityFieldRegistry.ENTITY_TYPES.stream().anyMatch(e -> e.code().equals(entityType))) {
            return "redirect:/admin/extrafields";
        }
        List<ExtraFieldDefinition> definitions = extraFieldService.getDefinitions(entityType);
        EntityFieldRegistry.EntityMeta meta = EntityFieldRegistry.ENTITY_TYPES.stream()
                .filter(e -> e.code().equals(entityType)).findFirst().orElse(null);
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityTypeLabel", meta != null ? meta.label() : entityType);
        model.addAttribute("definitions", definitions);
        return "modules/admin/extrafield-list";
    }

    @GetMapping("/{entityType}/new")
    public String newForm(@PathVariable String entityType, Model model) {
        if (!EntityFieldRegistry.ENTITY_TYPES.stream().anyMatch(e -> e.code().equals(entityType))) {
            return "redirect:/admin/extrafields";
        }
        ExtraFieldDefinitionDto dto = new ExtraFieldDefinitionDto();
        dto.setFieldType("TEXT");
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityTypeLabel", EntityFieldRegistry.ENTITY_TYPES.stream()
                .filter(e -> e.code().equals(entityType)).findFirst().map(EntityFieldRegistry.EntityMeta::label).orElse(entityType));
        model.addAttribute("definition", dto);
        model.addAttribute("fieldTypes", getFieldTypes());
        return "modules/admin/extrafield-form";
    }

    @PostMapping("/{entityType}/new")
    public String create(@PathVariable String entityType,
                         @Valid @ModelAttribute("definition") ExtraFieldDefinitionDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (!EntityFieldRegistry.ENTITY_TYPES.stream().anyMatch(e -> e.code().equals(entityType))) {
            return "redirect:/admin/extrafields";
        }
        if (result.hasErrors()) {
            model.addAttribute("entityType", entityType);
            model.addAttribute("entityTypeLabel", EntityFieldRegistry.ENTITY_TYPES.stream()
                    .filter(e -> e.code().equals(entityType)).findFirst().map(EntityFieldRegistry.EntityMeta::label).orElse(entityType));
            model.addAttribute("fieldTypes", getFieldTypes());
            return "modules/admin/extrafield-form";
        }
        try {
            extraFieldService.createDefinition(entityType, dto);
            ra.addFlashAttribute("successMessage", "Extra field created.");
        } catch (IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("entityType", entityType);
            model.addAttribute("entityTypeLabel", EntityFieldRegistry.ENTITY_TYPES.stream()
                    .filter(m -> m.code().equals(entityType)).findFirst().map(EntityFieldRegistry.EntityMeta::label).orElse(entityType));
            model.addAttribute("fieldTypes", getFieldTypes());
            return "modules/admin/extrafield-form";
        }
        return "redirect:/admin/extrafields/" + entityType;
    }

    @GetMapping("/{entityType}/{id}/edit")
    public String editForm(@PathVariable String entityType, @PathVariable Long id, Model model) {
        ExtraFieldDefinition def = extraFieldService.getDefinitionById(id);
        if (!def.getEntityType().equals(entityType)) {
            return "redirect:/admin/extrafields/" + entityType;
        }
        ExtraFieldDefinitionDto dto = new ExtraFieldDefinitionDto();
        dto.setFieldCode(def.getFieldCode());
        dto.setLabel(def.getLabel());
        dto.setFieldType(def.getFieldType());
        dto.setFieldOptions(def.getFieldOptions());
        dto.setDefaultValue(def.getDefaultValue());
        dto.setRequired(def.isRequired());
        dto.setSortOrder(def.getSortOrder());
        dto.setVisibleOnList(def.isVisibleOnList());
        dto.setVisibleOnDetail(def.isVisibleOnDetail());
        dto.setVisibleOnForm(def.isVisibleOnForm());
        dto.setActive(def.isActive());
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityTypeLabel", EntityFieldRegistry.ENTITY_TYPES.stream()
                .filter(e -> e.code().equals(entityType)).findFirst().map(EntityFieldRegistry.EntityMeta::label).orElse(entityType));
        model.addAttribute("definition", dto);
        model.addAttribute("definitionId", id);
        model.addAttribute("fieldTypes", getFieldTypes());
        return "modules/admin/extrafield-form";
    }

    @PostMapping("/{entityType}/{id}/edit")
    public String update(@PathVariable String entityType, @PathVariable Long id,
                         @Valid @ModelAttribute("definition") ExtraFieldDefinitionDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("entityType", entityType);
            model.addAttribute("entityTypeLabel", EntityFieldRegistry.ENTITY_TYPES.stream()
                    .filter(e -> e.code().equals(entityType)).findFirst().map(EntityFieldRegistry.EntityMeta::label).orElse(entityType));
            model.addAttribute("definitionId", id);
            model.addAttribute("fieldTypes", getFieldTypes());
            return "modules/admin/extrafield-form";
        }
        try {
            extraFieldService.updateDefinition(id, dto);
            ra.addFlashAttribute("successMessage", "Extra field updated.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/extrafields/" + entityType;
    }

    @PostMapping("/{entityType}/{id}/delete")
    public String delete(@PathVariable String entityType, @PathVariable Long id, RedirectAttributes ra) {
        extraFieldService.deleteDefinition(id);
        ra.addFlashAttribute("successMessage", "Extra field deleted.");
        return "redirect:/admin/extrafields/" + entityType;
    }

    private static List<String[]> getFieldTypes() {
        return List.of(
                new String[]{"TEXT", "Text"},
                new String[]{"VARCHAR", "Short text"},
                new String[]{"INTEGER", "Integer"},
                new String[]{"DECIMAL", "Decimal"},
                new String[]{"BOOLEAN", "Yes/No"},
                new String[]{"DATE", "Date"},
                new String[]{"DATETIME", "Date & time"},
                new String[]{"EMAIL", "Email"},
                new String[]{"URL", "URL"},
                new String[]{"PHONE", "Phone"},
                new String[]{"SELECT", "Select list"}
        );
    }
}
