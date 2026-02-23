package fr.aplose.erp.modules.nocode.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoCodeMenuEntry {
    private String moduleCode;
    private String objectCode;
    private String label;
    private String icon;
    private String path;
}
