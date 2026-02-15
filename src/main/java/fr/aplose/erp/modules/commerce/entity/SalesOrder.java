package fr.aplose.erp.modules.commerce.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_orders")
@Getter
@Setter
@NoArgsConstructor
public class SalesOrder extends BaseEntity {

    @Column(name = "reference", length = 50, nullable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "CONFIRMED";

    @Column(name = "date_ordered", nullable = false)
    private LocalDate dateOrdered;

    @Column(name = "date_expected")
    private LocalDate dateExpected;

    @Column(name = "date_delivered")
    private LocalDate dateDelivered;

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

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_rep_id")
    private User salesRep;

    @Column(name = "created_by")
    private Long createdById;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SalesOrderLine> lines = new ArrayList<>();

    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (SalesOrderLine line : lines) {
            line.recalculate();
            sub = sub.add(line.getLineTotal());
            vat = vat.add(line.getLineTotal().multiply(line.getVatRate()));
        }
        this.subtotal = sub;
        this.vatAmount = vat;
        this.totalAmount = sub.subtract(discountAmount).add(vat);
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case "CONFIRMED" -> "bg-primary";
            case "PROCESSING" -> "bg-warning text-dark";
            case "SHIPPED" -> "bg-info";
            case "DELIVERED" -> "bg-success";
            case "CANCELLED" -> "bg-dark";
            default -> "bg-secondary";
        };
    }
}
