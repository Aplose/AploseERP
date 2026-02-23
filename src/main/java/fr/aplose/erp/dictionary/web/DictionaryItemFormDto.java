package fr.aplose.erp.dictionary.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictionaryItemFormDto {

    private String code;
    private String label;
    private Integer sortOrder;
    private Boolean active = true;
}
