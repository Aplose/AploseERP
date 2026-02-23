package fr.aplose.erp.dictionary.web;

import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.entity.DictionaryItem;
import fr.aplose.erp.dictionary.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/dictionaries")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TENANT_READ')")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    private static Map<String, String> getTypeLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(DictionaryType.CIVILITY, "dictionary.type.CIVILITY");
        m.put(DictionaryType.COUNTRY, "dictionary.type.COUNTRY");
        m.put(DictionaryType.CURRENCY, "dictionary.type.CURRENCY");
        m.put(DictionaryType.LEGAL_FORM, "dictionary.type.LEGAL_FORM");
        m.put(DictionaryType.PAYMENT_METHOD, "dictionary.type.PAYMENT_METHOD");
        return m;
    }

    @GetMapping
    public String listTypes(Model model) {
        model.addAttribute("typeLabels", getTypeLabels());
        return "modules/admin/dictionary-types";
    }

    @GetMapping("/{type}")
    public String listItems(@PathVariable String type, Model model) {
        if (!Arrays.asList(DictionaryType.ALL).contains(type)) {
            return "redirect:/admin/dictionaries";
        }
        model.addAttribute("dictionaryType", type);
        model.addAttribute("typeLabelKey", "dictionary.type." + type);
        model.addAttribute("items", dictionaryService.findByTypeIncludeInactive(type));
        return "modules/admin/dictionary-items";
    }

    @GetMapping("/{type}/new")
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String newItemForm(@PathVariable String type, Model model) {
        if (!Arrays.asList(DictionaryType.ALL).contains(type)) {
            return "redirect:/admin/dictionaries";
        }
        model.addAttribute("dictionaryType", type);
        model.addAttribute("typeLabelKey", "dictionary.type." + type);
        model.addAttribute("item", new DictionaryItemFormDto());
        return "modules/admin/dictionary-item-form";
    }

    @PostMapping("/{type}/new")
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String createItem(@PathVariable String type,
                             @ModelAttribute("item") DictionaryItemFormDto dto,
                             RedirectAttributes ra) {
        if (!Arrays.asList(DictionaryType.ALL).contains(type)) {
            return "redirect:/admin/dictionaries";
        }
        try {
            dictionaryService.create(type, dto.getCode(), dto.getLabel(),
                    dto.getSortOrder() != null ? dto.getSortOrder().intValue() : null,
                    dto.getActive());
            ra.addFlashAttribute("successMessage", "Dictionary item created.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/dictionaries/" + type + "/new";
        }
        return "redirect:/admin/dictionaries/" + type;
    }

    @GetMapping("/{type}/{id}/edit")
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String editItemForm(@PathVariable String type, @PathVariable Long id, Model model) {
        DictionaryItem item = dictionaryService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + id));
        if (!item.getType().equals(type)) {
            return "redirect:/admin/dictionaries/" + type;
        }
        DictionaryItemFormDto dto = new DictionaryItemFormDto();
        dto.setCode(item.getCode());
        dto.setLabel(item.getLabel());
        dto.setSortOrder((int) item.getSortOrder());
        dto.setActive(item.isActive());
        model.addAttribute("dictionaryType", type);
        model.addAttribute("typeLabelKey", "dictionary.type." + type);
        model.addAttribute("item", dto);
        model.addAttribute("itemId", id);
        return "modules/admin/dictionary-item-form";
    }

    @PostMapping("/{type}/{id}/edit")
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String updateItem(@PathVariable String type, @PathVariable Long id,
                             @ModelAttribute("item") DictionaryItemFormDto dto,
                             RedirectAttributes ra) {
        try {
            dictionaryService.update(id, dto.getCode(), dto.getLabel(),
                    dto.getSortOrder() != null ? dto.getSortOrder().intValue() : null,
                    dto.getActive());
            ra.addFlashAttribute("successMessage", "Dictionary item updated.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dictionaries/" + type;
    }

    @PostMapping("/{type}/{id}/delete")
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String deleteItem(@PathVariable String type, @PathVariable Long id,
                             RedirectAttributes ra) {
        dictionaryService.delete(id);
        ra.addFlashAttribute("successMessage", "Dictionary item deleted.");
        return "redirect:/admin/dictionaries/" + type;
    }
}
