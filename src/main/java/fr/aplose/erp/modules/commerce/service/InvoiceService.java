package fr.aplose.erp.modules.commerce.service;

import fr.aplose.erp.modules.automation.service.AutomationRuleService;
import fr.aplose.erp.modules.catalog.repository.ProductRepository;
import fr.aplose.erp.modules.webhook.service.WebhookService;
import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.entity.InvoiceLine;
import fr.aplose.erp.modules.commerce.entity.Payment;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.commerce.web.dto.InvoiceDto;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.commerce.web.dto.PaymentDto;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository repo;
    private final ThirdPartyRepository thirdPartyRepo;
    private final ContactRepository contactRepo;
    private final ProductRepository productRepo;
    private final AutomationRuleService automationRuleService;
    private final WebhookService webhookService;

    @Transactional(readOnly = true)
    public Page<Invoice> findAll(String q, String type, String status, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return repo.search(tid, q, pageable);
        if (status != null && !status.isBlank()) return repo.findByTenantIdAndStatus(tid, status, pageable);
        if (type != null && !type.isBlank()) return repo.findByTenantIdAndType(tid, type, pageable);
        return repo.findByTenantId(tid, pageable);
    }

    @Transactional(readOnly = true)
    public Invoice findById(Long id) {
        return repo.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    @Transactional
    public Invoice create(InvoiceDto dto, Long currentUserId) {
        String tid = TenantContext.getCurrentTenantId();

        Invoice inv = new Invoice();
        inv.setReference(generateReference(tid, dto.getType()));
        inv.setCreatedById(currentUserId);
        applyDto(inv, dto, tid);
        inv = repo.save(inv);
        webhookService.trigger(tid, "INVOICE.CREATED", invoicePayload(inv));
        return inv;
    }

    @Transactional
    public Invoice update(Long id, InvoiceDto dto) {
        Invoice inv = findById(id);
        if (!"DRAFT".equals(inv.getStatus())) {
            throw new IllegalStateException("Only draft invoices can be edited");
        }
        applyDto(inv, dto, TenantContext.getCurrentTenantId());
        return repo.save(inv);
    }

    @Transactional
    public InvoiceLine addLine(Long invoiceId, LineDto dto) {
        Invoice inv = findById(invoiceId);
        if (!"DRAFT".equals(inv.getStatus())) {
            throw new IllegalStateException("Only draft invoices can have lines added");
        }
        String tid = TenantContext.getCurrentTenantId();

        InvoiceLine line = new InvoiceLine();
        line.setTenantId(tid);
        line.setInvoice(inv);
        line.setDescription(dto.getDescription());
        line.setQuantity(dto.getQuantity());
        line.setUnitPrice(dto.getUnitPrice());
        line.setDiscountPct(dto.getDiscountPct());
        line.setVatRate(dto.getVatRate());
        line.setSortOrder((short) inv.getLines().size());

        if (dto.getProductId() != null) {
            productRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getProductId(), tid)
                    .ifPresent(line::setProduct);
        }

        line.recalculate();
        inv.getLines().add(line);
        inv.recalculate();
        repo.save(inv);
        return line;
    }

    @Transactional
    public void removeLine(Long invoiceId, Long lineId) {
        Invoice inv = findById(invoiceId);
        if (!"DRAFT".equals(inv.getStatus())) {
            throw new IllegalStateException("Only draft invoices can have lines removed");
        }
        inv.getLines().removeIf(l -> l.getId().equals(lineId));
        inv.recalculate();
        repo.save(inv);
    }

    @Transactional
    public void validate(Long id, Long userId) {
        Invoice inv = findById(id);
        if (!"DRAFT".equals(inv.getStatus())) {
            throw new IllegalStateException("Only draft invoices can be validated");
        }
        if (inv.getLines().isEmpty()) {
            throw new IllegalStateException("Invoice must have at least one line");
        }
        inv.setStatus("VALIDATED");
        inv.setValidatedAt(LocalDateTime.now());
        inv.setValidatedById(userId);
        repo.save(inv);

        Map<String, Object> context = new HashMap<>();
        context.put("status", "VALIDATED");
        context.put("amount", inv.getTotalAmount());
        context.put("entityId", inv.getId());
        context.put("thirdPartyId", inv.getThirdParty() != null ? inv.getThirdParty().getId() : null);
        automationRuleService.runRules("INVOICE", "VALIDATED", context);
        webhookService.trigger(inv.getTenantId(), "INVOICE.VALIDATED", invoicePayload(inv));
    }

    @Transactional
    public Payment addPayment(Long invoiceId, PaymentDto dto, Long userId) {
        Invoice inv = findById(invoiceId);
        if ("DRAFT".equals(inv.getStatus()) || "CANCELLED".equals(inv.getStatus())) {
            throw new IllegalStateException("Cannot add payment to a " + inv.getStatus() + " invoice");
        }

        Payment payment = new Payment();
        payment.setTenantId(TenantContext.getCurrentTenantId());
        payment.setInvoice(inv);
        payment.setAmount(dto.getAmount());
        payment.setCurrencyCode(inv.getCurrencyCode());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setReference(dto.getReference());
        payment.setNotes(dto.getNotes());
        payment.setCreatedById(userId);

        inv.getPayments().add(payment);

        BigDecimal totalPaid = inv.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        inv.setAmountPaid(totalPaid);
        inv.setAmountRemaining(inv.getTotalAmount().subtract(totalPaid));

        if (inv.getAmountRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            inv.setStatus("PAID");
        } else {
            inv.setStatus("PARTIALLY_PAID");
        }

        repo.save(inv);
        return payment;
    }

    @Transactional
    public void cancel(Long id) {
        Invoice inv = findById(id);
        inv.setStatus("CANCELLED");
        repo.save(inv);
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return repo.countByTenantIdAndStatusIn(TenantContext.getCurrentTenantId(),
                Set.of("DRAFT", "VALIDATED", "SENT", "PARTIALLY_PAID"));
    }

    @Transactional(readOnly = true)
    public List<Invoice> findOverdue() {
        return repo.findOverdue(TenantContext.getCurrentTenantId(), LocalDate.now());
    }

    private String generateReference(String tid, String type) {
        String prefix = "SALES".equals(type) ? "INV-%" : "BIL-%";
        int max = repo.findMaxReferenceNumber(tid, prefix);
        String pre = "SALES".equals(type) ? "INV-" : "BIL-";
        return String.format("%s%05d", pre, max + 1);
    }

    private void applyDto(Invoice inv, InvoiceDto dto, String tid) {
        inv.setType(dto.getType());
        thirdPartyRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getThirdPartyId(), tid)
                .ifPresent(inv::setThirdParty);
        if (dto.getContactId() != null) {
            contactRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getContactId(), tid)
                    .ifPresent(inv::setContact);
        } else {
            inv.setContact(null);
        }
        inv.setDateIssued(dto.getDateIssued());
        inv.setDateDue(dto.getDateDue());
        inv.setCurrencyCode(dto.getCurrencyCode());
        inv.setDiscountAmount(dto.getDiscountAmount());
        inv.setPaymentMethod(dto.getPaymentMethod());
        inv.setBankAccount(dto.getBankAccount());
        inv.setNotes(dto.getNotes());
        inv.setTerms(dto.getTerms());
    }

    private Map<String, Object> invoicePayload(Invoice inv) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityId", inv.getId());
        data.put("reference", inv.getReference());
        data.put("status", inv.getStatus());
        data.put("type", inv.getType());
        data.put("totalAmount", inv.getTotalAmount());
        data.put("currencyCode", inv.getCurrencyCode());
        data.put("dateIssued", inv.getDateIssued() != null ? inv.getDateIssued().toString() : null);
        data.put("dateDue", inv.getDateDue() != null ? inv.getDateDue().toString() : null);
        data.put("thirdPartyId", inv.getThirdParty() != null ? inv.getThirdParty().getId() : null);
        return data;
    }
}
