package fr.aplose.erp.modules.extrafield.web.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExtraFieldDefinitionDto {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "Must start with a letter and contain only lowercase letters, numbers, and underscores")
    private String fieldCode;

    @NotBlank
    @Size(max = 255)
    private String label;

    @NotBlank
    private String fieldType = "VARCHAR";

    private String fieldOptions;
    private String defaultValue;
    private boolean required = false;
    private short sortOrder = 0;
    private boolean visibleOnList = false;
    private boolean visibleOnDetail = true;
    private boolean visibleOnForm = true;
    private boolean active = true;
}
