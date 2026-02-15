package fr.aplose.erp.modules.thirdparty.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ThirdPartyDto {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    private boolean customer = false;
    private boolean supplier = false;
    private boolean prospect = false;

    @Size(max = 100)
    private String legalForm;

    @Size(max = 100)
    private String taxId;

    @Size(max = 100)
    private String registrationNo;

    @Size(max = 255)
    private String website;

    @Size(max = 50)
    private String phone;

    @Size(max = 50)
    private String fax;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String stateProvince;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 2)
    private String countryCode;

    @Size(max = 3)
    private String currencyCode;

    private Short paymentTerms = 30;
    private BigDecimal creditLimit;

    @Size(max = 500)
    private String tags;

    private String notes;
    private String status = "ACTIVE";
    private Long salesRepId;
    private Long parentId;
}
