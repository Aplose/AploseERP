package fr.aplose.erp.modules.commerce.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LineDto {

    private Long productId;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotNull
    @DecimalMin("0.0001")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0")
    private BigDecimal unitPrice;

    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal discountPct = BigDecimal.ZERO;

    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal vatRate = BigDecimal.ZERO;
}
