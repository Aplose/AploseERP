package fr.aplose.erp.modules.commerce.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SalesOrderDto {

    @NotNull
    private Long thirdPartyId;

    private Long contactId;

    private Long proposalId;

    @NotNull
    private LocalDate dateOrdered;

    private LocalDate dateExpected;

    @NotBlank
    @Size(max = 3)
    private String currencyCode = "EUR";

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String notes;
    private String terms;
    private Long salesRepId;
}
