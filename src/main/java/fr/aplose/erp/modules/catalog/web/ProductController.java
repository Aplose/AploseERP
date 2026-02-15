package fr.aplose.erp.modules.catalog.web;

import fr.aplose.erp.modules.catalog.service.ProductCategoryService;
import fr.aplose.erp.modules.catalog.service.ProductService;
import fr.aplose.erp.modules.catalog.web.dto.ProductDto;
import fr.aplose.erp.modules.catalog.repository.CurrencyRepository;
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
@RequestMapping("/products")
@PreAuthorize("hasAuthority('PRODUCT_READ')")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;
    private final ProductCategoryService categoryService;
    private final CurrencyRepository currencyRepository;

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
        return "modules/catalog/product-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("product", new ProductDto());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
        return "modules/catalog/product-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public String create(@Valid @ModelAttribute("product") ProductDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
            return "modules/catalog/product-form";
        }
        try {
            var p = service.create(dto);
            ra.addFlashAttribute("successMessage", "Product created");
            return "redirect:/products/" + p.getId();
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
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
        model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
        return "modules/catalog/product-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("product") ProductDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
            return "modules/catalog/product-form";
        }
        try {
            service.update(id, dto);
            ra.addFlashAttribute("successMessage", "Product updated");
            return "redirect:/products/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("productId", id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("currencies", currencyRepository.findByActiveTrueOrderByCodeAsc());
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
