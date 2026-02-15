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
@Table(name = "proposals")
@Getter
@Setter
@NoArgsConstructor
public class Proposal extends BaseEntity {

    @Column(name = "reference", length = 50, nullable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(name = "title")
    private String title;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "DRAFT";

    @Column(name = "date_issued", nullable = false)
    private LocalDate dateIssued;

    @Column(name = "date_valid_until")
    private LocalDate dateValidUntil;

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

    @Column(name = "converted_to_order_id")
    private Long convertedToOrderId;

    @Column(name = "created_by")
    private Long createdById;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProposalLine> lines = new ArrayList<>();

    public void recalculate() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (ProposalLine line : lines) {
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
            case "DRAFT" -> "bg-secondary";
            case "SENT" -> "bg-primary";
            case "ACCEPTED" -> "bg-success";
            case "REFUSED" -> "bg-danger";
            case "CANCELLED" -> "bg-dark";
            case "CONVERTED" -> "bg-info";
            default -> "bg-secondary";
        };
    }
}
