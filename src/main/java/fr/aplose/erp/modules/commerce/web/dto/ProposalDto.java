package fr.aplose.erp.modules.commerce.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ProposalDto {

    @NotNull
    private Long thirdPartyId;

    private Long contactId;

    @Size(max = 255)
    private String title;

    @NotNull
    private LocalDate dateIssued;

    private LocalDate dateValidUntil;

    @NotBlank
    @Size(max = 3)
    private String currencyCode = "EUR";

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private String notes;
    private String terms;
    private Long salesRepId;
}
