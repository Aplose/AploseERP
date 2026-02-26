package fr.aplose.erp.api.v1.web;

import fr.aplose.erp.api.v1.dto.InvoiceLineResponseDto;
import fr.aplose.erp.api.v1.dto.InvoiceResponseDto;
import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.entity.InvoiceLine;
import fr.aplose.erp.modules.commerce.service.InvoiceService;
import fr.aplose.erp.modules.commerce.web.dto.InvoiceDto;
import fr.aplose.erp.security.service.ErpUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@PreAuthorize("hasAuthority('INVOICE_READ')")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Sales and purchase invoices")
public class InvoiceApiController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "List invoices")
    public Page<InvoiceResponseDto> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return invoiceService.findAll(q, type, status, pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceResponseDto> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(toResponse(invoiceService.findById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVOICE_CREATE')")
    @Operation(summary = "Create invoice")
    public ResponseEntity<InvoiceResponseDto> create(
            @Valid @RequestBody InvoiceDto dto,
            @AuthenticationPrincipal ErpUserDetails user) {
        Invoice created = invoiceService.create(dto, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVOICE_UPDATE')")
    @Operation(summary = "Update draft invoice")
    public ResponseEntity<InvoiceResponseDto> update(@PathVariable Long id, @Valid @RequestBody InvoiceDto dto) {
        try {
            Invoice updated = invoiceService.update(id, dto);
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('INVOICE_VALIDATE')")
    @Operation(summary = "Validate invoice")
    public ResponseEntity<InvoiceResponseDto> validate(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails user) {
        try {
            invoiceService.validate(id, user.getUserId());
            return ResponseEntity.ok(toResponse(invoiceService.findById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private InvoiceResponseDto toResponse(Invoice inv) {
        InvoiceResponseDto dto = new InvoiceResponseDto();
        dto.setId(inv.getId());
        dto.setReference(inv.getReference());
        dto.setType(inv.getType());
        dto.setThirdPartyId(inv.getThirdParty() != null ? inv.getThirdParty().getId() : null);
        dto.setStatus(inv.getStatus());
        dto.setDateIssued(inv.getDateIssued());
        dto.setDateDue(inv.getDateDue());
        dto.setCurrencyCode(inv.getCurrencyCode());
        dto.setTotalAmount(inv.getTotalAmount());
        dto.setAmountPaid(inv.getAmountPaid());
        dto.setAmountRemaining(inv.getAmountRemaining());
        dto.setCreatedAt(inv.getCreatedAt());
        for (InvoiceLine line : inv.getLines()) {
            InvoiceLineResponseDto ldto = new InvoiceLineResponseDto();
            ldto.setId(line.getId());
            ldto.setDescription(line.getDescription());
            ldto.setQuantity(line.getQuantity());
            ldto.setUnitPrice(line.getUnitPrice());
            ldto.setLineTotal(line.getLineTotal());
            dto.getLines().add(ldto);
        }
        return dto;
    }
}
