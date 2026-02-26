package fr.aplose.erp.modules.publicform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * One field definition for a public form (from fields_json).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormFieldDto {
    private String name;
    private String label;
    private String type;   // text, email, textarea, tel, number
    private boolean required;
    private String placeholder;

    public static FormFieldDto fromMap(Map<String, Object> m) {
        FormFieldDto dto = new FormFieldDto();
        dto.setName(m.get("name") != null ? m.get("name").toString() : "");
        dto.setLabel(m.get("label") != null ? m.get("label").toString() : "");
        dto.setType(m.get("type") != null ? m.get("type").toString() : "text");
        dto.setRequired(Boolean.TRUE.equals(m.get("required")));
        dto.setPlaceholder(m.get("placeholder") != null ? m.get("placeholder").toString() : null);
        return dto;
    }
}
