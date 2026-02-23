package fr.aplose.erp.modules.contact.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ContactThirdPartyLinkDto {

    private Long thirdPartyId;
    private String linkTypeCode = "SALARIE";

    /** For display only (e.g. in detail view). */
    private String thirdPartyName;
}
