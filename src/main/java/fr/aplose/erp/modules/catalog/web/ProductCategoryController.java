package fr.aplose.erp.modules.catalog.web;

import fr.aplose.erp.modules.catalog.service.ProductCategoryService;
import fr.aplose.erp.modules.catalog.web.dto.ProductCategoryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products/categories")
@PreAuthorize("hasAuthority('PRODUCT_READ')")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService service;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", service.findAll());
        return "modules/catalog/category-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("category", new ProductCategoryDto());
        model.addAttribute("parentCategories", service.findAll());
        return "modules/catalog/category-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String create(@Valid @ModelAttribute("category") ProductCategoryDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", service.findAll());
            return "modules/catalog/category-form";
        }
        try {
            service.create(dto);
            ra.addFlashAttribute("successMessage", "Category created");
            return "redirect:/products/categories";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("parentCategories", service.findAll());
            return "modules/catalog/category-form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var cat = service.findById(id);
        var dto = new ProductCategoryDto();
        dto.setCode(cat.getCode()); dto.setName(cat.getName());
        dto.setDescription(cat.getDescription()); dto.setSortOrder(cat.getSortOrder());
        dto.setParentId(cat.getParent() != null ? cat.getParent().getId() : null);
        model.addAttribute("category", dto);
        model.addAttribute("categoryId", id);
        model.addAttribute("parentCategories", service.findAll());
        return "modules/catalog/category-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("category") ProductCategoryDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categoryId", id);
            model.addAttribute("parentCategories", service.findAll());
            return "modules/catalog/category-form";
        }
        try {
            service.update(id, dto);
            ra.addFlashAttribute("successMessage", "Category updated");
            return "redirect:/products/categories";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categoryId", id);
            model.addAttribute("parentCategories", service.findAll());
            return "modules/catalog/category-form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("successMessage", "Category deleted");
        return "redirect:/products/categories";
    }
}
