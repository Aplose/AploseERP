package fr.aplose.erp.security.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfilePasswordDto {

    @NotBlank(message = "{profile.password.current.required}")
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
