package fr.aplose.erp.modules.contact.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
public class Contact extends BaseEntity {

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContactThirdPartyLink> thirdPartyLinks = new ArrayList<>();

    @Column(name = "civility", length = 10)
    private String civility;

    @Column(name = "first_name", length = 100, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "email")
    private String email;

    @Column(name = "email_secondary")
    private String emailSecondary;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "mobile", length = 50)
    private String phoneMobile;

    @Column(name = "fax", length = 50)
    private String fax;

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

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public String getFullName() {
        if (lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }
}
