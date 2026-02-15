package fr.aplose.erp.modules.fieldconfig.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldVisibilityDto {

    private String fieldName;
    private boolean visibleOnList = true;
    private boolean visibleOnDetail = true;
    private boolean visibleOnForm = true;
    private short sortOrder = 0;
}
