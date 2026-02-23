package fr.aplose.erp.modules.commerce.web;

import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.modules.commerce.entity.SalesOrder;
import fr.aplose.erp.modules.commerce.service.SalesOrderService;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.commerce.web.dto.SalesOrderDto;
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
@RequestMapping("/orders")
@PreAuthorize("hasAuthority('SALES_ORDER_READ')")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService service;
    private final DictionaryService dictionaryService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                      @RequestParam(defaultValue = "") String status,
                      @RequestParam(defaultValue = "0") int page,
                      Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "dateOrdered"));
        model.addAttribute("orders", service.findAll(q, status, pageable));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "modules/commerce/order-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SalesOrder order = service.findById(id);
        model.addAttribute("order", order);
        model.addAttribute("newLine", new LineDto());
        return "modules/commerce/order-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('SALES_ORDER_CREATE')")
    public String newForm(Model model) {
        var dto = new SalesOrderDto();
        dto.setDateOrdered(java.time.LocalDate.now());
        model.addAttribute("order", dto);
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        return "modules/commerce/order-form";
    }

    @GetMapping("/from-proposal/{proposalId}")
    @PreAuthorize("hasAuthority('SALES_ORDER_CREATE')")
    public String createFromProposal(@PathVariable Long proposalId,
                                    @AuthenticationPrincipal ErpUserDetails principal,
                                    RedirectAttributes ra) {
        SalesOrder order = service.createFromProposal(proposalId, principal.getUserId());
        ra.addFlashAttribute("successMessage", "Order " + order.getReference() + " created from proposal");
        return "redirect:/orders/" + order.getId();
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('SALES_ORDER_CREATE')")
    public String create(@Valid @ModelAttribute("order") SalesOrderDto dto,
                        BindingResult result,
                        @AuthenticationPrincipal ErpUserDetails principal,
                        Model model,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/commerce/order-form";
        }
        try {
            SalesOrder o = service.create(dto, principal.getUserId());
            ra.addFlashAttribute("successMessage", "Order " + o.getReference() + " created");
            return "redirect:/orders/" + o.getId();
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/commerce/order-form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        SalesOrder o = service.findById(id);
        var dto = new SalesOrderDto();
        dto.setThirdPartyId(o.getThirdParty().getId());
        dto.setContactId(o.getContact() != null ? o.getContact().getId() : null);
        dto.setProposalId(o.getProposal() != null ? o.getProposal().getId() : null);
        dto.setDateOrdered(o.getDateOrdered());
        dto.setDateExpected(o.getDateExpected());
        dto.setCurrencyCode(o.getCurrencyCode());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setNotes(o.getNotes());
        dto.setTerms(o.getTerms());
        dto.setSalesRepId(o.getSalesRep() != null ? o.getSalesRep().getId() : null);
        model.addAttribute("order", dto);
        model.addAttribute("orderId", id);
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        return "modules/commerce/order-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("order") SalesOrderDto dto,
                        BindingResult result,
                        Model model,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("orderId", id);
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/commerce/order-form";
        }
        try {
            service.update(id, dto);
            ra.addFlashAttribute("successMessage", "Order updated");
            return "redirect:/orders/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("orderId", id);
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/commerce/order-form";
        }
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    public String addLine(@PathVariable Long id,
                          @Valid @ModelAttribute("newLine") LineDto dto,
                          RedirectAttributes ra) {
        try {
            service.addLine(id, dto);
            ra.addFlashAttribute("successMessage", "Line added");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/lines/{lineId}/delete")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    public String removeLine(@PathVariable Long id, @PathVariable Long lineId, RedirectAttributes ra) {
        try {
            service.removeLine(id, lineId);
            ra.addFlashAttribute("successMessage", "Line removed");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SALES_ORDER_UPDATE')")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes ra) {
        service.updateStatus(id, status);
        ra.addFlashAttribute("successMessage", "Order status updated");
        return "redirect:/orders/" + id;
    }
}
