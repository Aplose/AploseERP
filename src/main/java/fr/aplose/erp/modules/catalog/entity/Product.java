package fr.aplose.erp.modules.catalog.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", length = 20, nullable = false)
    private String type = "PRODUCT";

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "sale_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(name = "purchase_price", precision = 19, scale = 4, nullable = false)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "EUR";

    @Column(name = "vat_rate", precision = 6, scale = 4, nullable = false)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "is_sellable", nullable = false)
    private boolean sellable = true;

    @Column(name = "is_purchasable", nullable = false)
    private boolean purchasable = true;

    @Column(name = "track_stock", nullable = false)
    private boolean trackStock = false;

    @Column(name = "stock_quantity", precision = 19, scale = 4, nullable = false)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(name = "stock_alert_level", precision = 19, scale = 4)
    private BigDecimal stockAlertLevel;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public String getDisplayType() {
        return switch (type) {
            case "SERVICE" -> "Service";
            case "CONSUMABLE" -> "Consumable";
            default -> "Product";
        };
    }

    public BigDecimal getMargin() {
        if (purchasePrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return salePrice.subtract(purchasePrice);
    }
}
