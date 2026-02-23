package fr.aplose.erp.modules.project.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ProjectDto {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    private Long thirdPartyId;

    @NotBlank
    @Size(max = 30)
    private String status = "PLANNING";

    @NotBlank
    @Size(max = 20)
    private String priority = "MEDIUM";

    private LocalDate dateStart;
    private LocalDate dateEnd;

    private BigDecimal budgetAmount;

    @Size(max = 3)
    private String currencyCode;

    private Long managerId;

    @Size(max = 30)
    private String billingMode = "FIXED";

    private String notes;
}
