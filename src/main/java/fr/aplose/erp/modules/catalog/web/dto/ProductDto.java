package fr.aplose.erp.modules.catalog.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductDto {

    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank
    private String type = "PRODUCT";

    @Size(max = 50)
    private String unitOfMeasure;

    @NotNull
    @DecimalMin("0")
    private BigDecimal salePrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @NotBlank
    @Size(max = 3)
    private String currencyCode = "EUR";

    @NotNull
    @DecimalMin("0")
    private BigDecimal vatRate = BigDecimal.ZERO;

    private boolean sellable = true;
    private boolean purchasable = true;
    private boolean trackStock = false;
    private BigDecimal stockAlertLevel;

    @Size(max = 100)
    private String barcode;

    private String notes;
    private boolean active = true;
    private Long categoryId;
}
