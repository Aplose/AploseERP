package fr.aplose.erp.modules.commerce.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "business_contracts")
@Getter
@Setter
@NoArgsConstructor
public class BusinessContract extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "third_party_id", nullable = false)
    private ThirdParty thirdParty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @NotBlank
    @Column(name = "contract_type", length = 50, nullable = false)
    private String contractType;

    @Column(name = "reference", length = 50)
    private String reference;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "renewal_type", length = 30)
    private String renewalType;

    @Column(name = "renewal_notice_days")
    private Integer renewalNoticeDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
