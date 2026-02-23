package fr.aplose.erp.tenant.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupDto {

    @NotBlank(message = "{signup.companyName.required}")
    @Size(max = 255)
    private String companyName;

    @Size(max = 50)
    private String companyCode;

    @NotBlank(message = "{signup.companyEmail.required}")
    @Email
    @Size(max = 255)
    private String companyEmail;

    @Size(max = 50)
    private String companyPhone;

    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String stateProvince;

    @Size(max = 2)
    private String countryCode;

    @Size(max = 100)
    private String registrationId;

    @NotBlank(message = "{signup.plan.required}")
    @Size(max = 50)
    private String plan = "discovery";

    @NotBlank(message = "{signup.adminEmail.required}")
    @Email
    @Size(max = 255)
    private String adminEmail;

    @Size(max = 100)
    private String adminFirstName;

    @Size(max = 100)
    private String adminLastName;

    @NotBlank(message = "{signup.password.required}")
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank(message = "{signup.confirmPassword.required}")
    private String confirmPassword;
}
