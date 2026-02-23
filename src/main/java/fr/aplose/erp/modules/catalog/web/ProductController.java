package fr.aplose.erp.modules.catalog.web;

import fr.aplose.erp.modules.catalog.service.ProductCategoryService;
import fr.aplose.erp.modules.catalog.service.ProductService;
import fr.aplose.erp.modules.catalog.web.dto.ProductDto;
import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.modules.extrafield.entity.ExtraFieldDefinition;
import fr.aplose.erp.modules.extrafield.service.ExtraFieldService;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/products")
@PreAuthorize("hasAuthority('PRODUCT_READ')")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;
    private final ProductCategoryService categoryService;
    private final DictionaryService dictionaryService;
    private final ExtraFieldService extraFieldService;

    private static final String ENTITY_TYPE_PRODUCT = "PRODUCT";

    private void addExtraFieldModelAttributes(Model model, Long entityId) {
        List<ExtraFieldDefinition> definitions = extraFieldService.getActiveDefinitions(ENTITY_TYPE_PRODUCT);
        model.addAttribute("extraFieldDefinitions", definitions);
        Map<String, String> values = entityId != null ? extraFieldService.getValues(ENTITY_TYPE_PRODUCT, entityId) : new LinkedHashMap<>();
        model.addAttribute("extraFieldValues", values);
    }

    private Map<String, String> getExtraFieldValuesFromRequest(HttpServletRequest request) {
        List<ExtraFieldDefinition> defs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_PRODUCT);
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
        model.addAttribute("extraFieldDefinitions", extraFieldService.getActiveDefinitions(ENTITY_TYPE_PRODUCT));
        model.addAttribute("extraFieldValues", getExtraFieldValuesFromRequest(request));
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String type,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by("name"));
        model.addAttribute("products", service.findAll(q, type, categoryId, pageable));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("categoryId", categoryId);
        return "modules/catalog/product-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", service.findById(id));
        List<ExtraFieldDefinition> detailDefs = extraFieldService.getActiveDefinitions(ENTITY_TYPE_PRODUCT).stream()
                .filter(ExtraFieldDefinition::isVisibleOnDetail).toList();
        model.addAttribute("extraFieldDefinitionsDetail", detailDefs);
        model.addAttribute("extraFieldValues", extraFieldService.getValues(ENTITY_TYPE_PRODUCT, id));
        return "modules/catalog/product-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("product", new ProductDto());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        addExtraFieldModelAttributes(model, null);
        return "modules/catalog/product-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String create(@Valid @ModelAttribute("product") ProductDto dto,
                         BindingResult result,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/catalog/product-form";
        }
        try {
            var p = service.create(dto);
            extraFieldService.saveValues(ENTITY_TYPE_PRODUCT, p.getId(), getExtraFieldValuesFromRequest(request));
            ra.addFlashAttribute("successMessage", "Product created");
            return "redirect:/products/" + p.getId();
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/catalog/product-form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var p = service.findById(id);
        var dto = new ProductDto();
        dto.setCode(p.getCode()); dto.setName(p.getName()); dto.setDescription(p.getDescription());
        dto.setType(p.getType()); dto.setUnitOfMeasure(p.getUnitOfMeasure());
        dto.setSalePrice(p.getSalePrice()); dto.setPurchasePrice(p.getPurchasePrice());
        dto.setCurrencyCode(p.getCurrencyCode()); dto.setVatRate(p.getVatRate());
        dto.setSellable(p.isSellable()); dto.setPurchasable(p.isPurchasable());
        dto.setTrackStock(p.isTrackStock()); dto.setStockAlertLevel(p.getStockAlertLevel());
        dto.setBarcode(p.getBarcode()); dto.setNotes(p.getNotes()); dto.setActive(p.isActive());
        dto.setCategoryId(p.getCategory() != null ? p.getCategory().getId() : null);
        model.addAttribute("product", dto);
        model.addAttribute("productId", id);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        addExtraFieldModelAttributes(model, id);
        return "modules/catalog/product-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("product") ProductDto dto,
                         BindingResult result,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/catalog/product-form";
        }
        try {
            service.update(id, dto);
            extraFieldService.saveValues(ENTITY_TYPE_PRODUCT, id, getExtraFieldValuesFromRequest(request));
            ra.addFlashAttribute("successMessage", "Product updated");
            return "redirect:/products/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            addExtraFieldModelAttributesFromRequest(model, request);
            return "modules/catalog/product-form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("successMessage", "Product deleted");
        return "redirect:/products";
    }
}
