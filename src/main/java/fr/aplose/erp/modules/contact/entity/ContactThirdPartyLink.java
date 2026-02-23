package fr.aplose.erp.modules.contact.entity;

import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "contact_third_party_links")
@Getter
@Setter
@NoArgsConstructor
public class ContactThirdPartyLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @Column(name = "link_type_code", length = 50, nullable = false)
    private String linkTypeCode = "SALARIE";
}
