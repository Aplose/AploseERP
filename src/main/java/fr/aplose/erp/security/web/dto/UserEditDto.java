package fr.aplose.erp.security.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class UserEditDto {

    @NotBlank
    @Email
    private String email;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 50)
    private String phone;

    private String locale;
    private String timezone;
    private boolean active = true;
    private boolean tenantAdmin = false;

    /** Responsable hiérarchique (User id). */
    private Long managerId;
    /** Validateur de congés (User id). */
    private Long leaveValidatorId;

    private Set<Long> roleIds = new HashSet<>();
}
