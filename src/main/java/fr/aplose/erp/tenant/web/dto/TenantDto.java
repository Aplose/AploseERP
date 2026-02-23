package fr.aplose.erp.tenant.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantDto {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String legalName;

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

    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String email;

    @Size(max = 255)
    private String website;

    @NotBlank
    @Size(max = 10)
    private String defaultLocale = "en";

    @NotBlank
    @Size(max = 3)
    private String defaultCurrency = "EUR";

    @NotBlank
    @Size(max = 100)
    private String timezone = "UTC";

    private short fiscalYearStart = 1;
}
