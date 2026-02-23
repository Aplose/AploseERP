package fr.aplose.erp.modules.commerce.entity;

import fr.aplose.erp.core.businessobject.BusinessObject;
import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends BaseEntity implements BusinessObject {

    @Column(name = "reference", length = 50, nullable = false)
    private String reference;

    @Column(name = "type", length = 30, nullable = false)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id")
    private SalesOrder salesOrder;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "DRAFT";

    @Column(name = "date_issued", nullable = false)
    private LocalDate dateIssued;

    @Column(name = "date_due", nullable = false)
    private LocalDate dateDue;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "exchange_rate", precision = 19, scale = 8, nullable = false)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "subtotal", precision = 19, scale = 4, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "vat_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_remaining", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountRemaining = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "bank_account", length = 100)
    private String bankAccount;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @Column(name = "validated_by")
    private Long validatedById;

    @Column(name = "created_by")
    private Long createdById;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    @OrderBy("paymentDate ASC")
    private List<Payment> payments = new ArrayList<>();

    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (InvoiceLine line : lines) {
            line.recalculate();
            sub = sub.add(line.getLineTotal());
            vat = vat.add(line.getLineTotal().multiply(line.getVatRate()));
        }
        this.subtotal = sub;
        this.vatAmount = vat;
        this.totalAmount = sub.subtract(discountAmount).add(vat);
        this.amountRemaining = this.totalAmount.subtract(this.amountPaid);
    }

    public boolean isOverdue() {
        return dateDue != null && dateDue.isBefore(LocalDate.now())
                && amountRemaining.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case "DRAFT" -> "bg-secondary";
            case "VALIDATED" -> "bg-primary";
            case "SENT" -> "bg-info";
            case "PAID" -> "bg-success";
            case "PARTIALLY_PAID" -> "bg-warning text-dark";
            case "CANCELLED" -> "bg-dark";
            default -> "bg-secondary";
        };
    }

    @Override
    public String getBusinessObjectTypeCode() {
        return "INVOICE";
    }
}
