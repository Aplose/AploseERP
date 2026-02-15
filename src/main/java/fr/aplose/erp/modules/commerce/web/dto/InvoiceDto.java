package fr.aplose.erp.modules.commerce.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class InvoiceDto {

    @NotBlank
    private String type = "SALES";

    @NotNull
    private Long thirdPartyId;

    private Long contactId;
    private Long salesOrderId;

    @NotNull
    private LocalDate dateIssued;

    @NotNull
    private LocalDate dateDue;

    @NotBlank
    @Size(max = 3)
    private String currencyCode = "EUR";

    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Size(max = 50)
    private String paymentMethod;

    @Size(max = 100)
    private String bankAccount;

    private String notes;
    private String terms;
}
