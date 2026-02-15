package fr.aplose.erp.modules.thirdparty.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "third_parties")
@Getter
@Setter
@NoArgsConstructor
public class ThirdParty extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", length = 30, nullable = false)
    private String type = "CUSTOMER";

    @Column(name = "is_customer", nullable = false)
    private boolean customer = false;

    @Column(name = "is_supplier", nullable = false)
    private boolean supplier = false;

    @Column(name = "is_prospect", nullable = false)
    private boolean prospect = false;

    @Column(name = "legal_form", length = 100)
    private String legalForm;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "registration_no", length = 100)
    private String registrationNo;

    @Column(name = "website")
    private String website;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "fax", length = 50)
    private String fax;

    @Column(name = "email")
    private String email;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String stateProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "payment_terms")
    private Short paymentTerms = 30;

    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_rep_id")
    private User salesRep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ThirdParty parent;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_by")
    private Long createdById;

    public String getDisplayType() {
        StringBuilder sb = new StringBuilder();
        if (customer) sb.append("Customer ");
        if (supplier) sb.append("Supplier ");
        if (prospect) sb.append("Prospect ");
        return sb.toString().trim().isEmpty() ? type : sb.toString().trim();
    }
}
