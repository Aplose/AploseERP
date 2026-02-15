package fr.aplose.erp.modules.commerce.web;

import fr.aplose.erp.modules.commerce.service.InvoiceService;
import fr.aplose.erp.modules.commerce.web.dto.InvoiceDto;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.commerce.web.dto.PaymentDto;
import fr.aplose.erp.modules.catalog.repository.CurrencyRepository;
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
@RequestMapping("/invoices")
@PreAuthorize("hasAuthority('INVOICE_READ')")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final CurrencyRepository currencyRepo;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String type,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "dateIssued"));
        model.addAttribute("invoices", service.findAll(q, type, status, pageable));
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        return "modules/commerce/invoice-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var inv = service.findById(id);
        model.addAttribute("invoice", inv);
        model.addAttribute("newLine", new LineDto());
        model.addAttribute("newPayment", new PaymentDto());
        return "modules/commerce/invoice-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public String newForm(Model model) {
        var dto = new InvoiceDto();
        dto.setDateIssued(java.time.LocalDate.now());
        dto.setDateDue(java.time.LocalDate.now().plusDays(30));
        model.addAttribute("invoice", dto);
        model.addAttribute("currencies", currencyRepo.findByActiveTrueOrderByCodeAsc());
        return "modules/commerce/invoice-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    public String create(@Valid @ModelAttribute("invoice") InvoiceDto dto,
                         BindingResult result,
                         @AuthenticationPrincipal ErpUserDetails principal,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", currencyRepo.findByActiveTrueOrderByCodeAsc());
            return "modules/commerce/invoice-form";
        }
        var inv = service.create(dto, principal.getUserId());
        ra.addFlashAttribute("successMessage", "Invoice " + inv.getReference() + " created");
        return "redirect:/invoices/" + inv.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('INVOICE_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var inv = service.findById(id);
        var dto = new InvoiceDto();
        dto.setType(inv.getType());
        dto.setThirdPartyId(inv.getThirdParty().getId());
        dto.setContactId(inv.getContact() != null ? inv.getContact().getId() : null);
        dto.setDateIssued(inv.getDateIssued());
        dto.setDateDue(inv.getDateDue());
        dto.setCurrencyCode(inv.getCurrencyCode());
        dto.setDiscountAmount(inv.getDiscountAmount());
        dto.setPaymentMethod(inv.getPaymentMethod());
        dto.setBankAccount(inv.getBankAccount());
        dto.setNotes(inv.getNotes());
        dto.setTerms(inv.getTerms());
        model.addAttribute("invoice", dto);
        model.addAttribute("invoiceId", id);
        model.addAttribute("currencies", currencyRepo.findByActiveTrueOrderByCodeAsc());
        return "modules/commerce/invoice-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('INVOICE_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("invoice") InvoiceDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("invoiceId", id);
            model.addAttribute("currencies", currencyRepo.findByActiveTrueOrderByCodeAsc());
            return "modules/commerce/invoice-form";
        }
        try {
            service.update(id, dto);
            ra.addFlashAttribute("successMessage", "Invoice updated");
            return "redirect:/invoices/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("invoiceId", id);
            model.addAttribute("currencies", currencyRepo.findByActiveTrueOrderByCodeAsc());
            return "modules/commerce/invoice-form";
        }
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('INVOICE_UPDATE')")
    public String addLine(@PathVariable Long id,
                          @Valid @ModelAttribute("newLine") LineDto dto,
                          RedirectAttributes ra) {
        try {
            service.addLine(id, dto);
            ra.addFlashAttribute("successMessage", "Line added");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/lines/{lineId}/delete")
    @PreAuthorize("hasAuthority('INVOICE_UPDATE')")
    public String removeLine(@PathVariable Long id, @PathVariable Long lineId, RedirectAttributes ra) {
        try {
            service.removeLine(id, lineId);
            ra.addFlashAttribute("successMessage", "Line removed");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('INVOICE_VALIDATE')")
    public String validate(@PathVariable Long id,
                           @AuthenticationPrincipal ErpUserDetails principal,
                           RedirectAttributes ra) {
        try {
            service.validate(id, principal.getUserId());
            ra.addFlashAttribute("successMessage", "Invoice validated");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    public String addPayment(@PathVariable Long id,
                             @Valid @ModelAttribute("newPayment") PaymentDto dto,
                             @AuthenticationPrincipal ErpUserDetails principal,
                             RedirectAttributes ra) {
        try {
            service.addPayment(id, dto, principal.getUserId());
            ra.addFlashAttribute("successMessage", "Payment recorded");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INVOICE_UPDATE')")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        service.cancel(id);
        ra.addFlashAttribute("successMessage", "Invoice cancelled");
        return "redirect:/invoices/" + id;
    }
}
