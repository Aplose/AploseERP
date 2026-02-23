package fr.aplose.erp.modules.contact.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ContactDto {

    private List<ContactThirdPartyLinkDto> links = new ArrayList<>();

    @Size(max = 10)
    private String civility;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 150)
    private String jobTitle;

    @Size(max = 100)
    private String department;

    @Email
    @Size(max = 255)
    private String email;

    @Email
    @Size(max = 255)
    private String emailSecondary;

    @Size(max = 50)
    private String phone;

    @Size(max = 50)
    private String phoneMobile;

    @Size(max = 50)
    private String fax;

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

    private String notes;
    private boolean primary = false;
    private String status = "ACTIVE";
}
