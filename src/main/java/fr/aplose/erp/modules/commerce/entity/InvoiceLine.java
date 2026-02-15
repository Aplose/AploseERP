package fr.aplose.erp.modules.commerce.entity;

import fr.aplose.erp.modules.catalog.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "invoice_lines")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder = 0;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_pct", precision = 6, scale = 4, nullable = false)
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "vat_rate", precision = 6, scale = 4, nullable = false)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 19, scale = 4, nullable = false)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public void recalculate() {
        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal discount = gross.multiply(discountPct).setScale(4, RoundingMode.HALF_UP);
        this.lineTotal = gross.subtract(discount).setScale(4, RoundingMode.HALF_UP);
    }
}
