package fr.aplose.erp.modules.commerce.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentDto {

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private LocalDate paymentDate;

    @NotBlank
    @Size(max = 50)
    private String paymentMethod;

    @Size(max = 100)
    private String reference;

    private String notes;
}
