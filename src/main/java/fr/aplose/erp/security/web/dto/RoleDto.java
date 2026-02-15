package fr.aplose.erp.security.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class RoleDto {

    @NotBlank
    @Size(min = 2, max = 100)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Role code must be uppercase letters, digits, and underscores only")
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String description;

    private Set<Long> permissionIds = new HashSet<>();
}
