package fr.aplose.erp.tenant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

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

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    @Column(name = "default_locale", length = 10, nullable = false)
    private String defaultLocale = "en";

    @Column(name = "default_currency", length = 3, nullable = false)
    private String defaultCurrency = "EUR";

    @Column(name = "fiscal_year_start", nullable = false)
    private short fiscalYearStart = 1;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "timezone", nullable = false)
    private String timezone = "UTC";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "plan", length = 50, nullable = false)
    private String plan = "trial";

    @Column(name = "registration_id", length = 100)
    private String registrationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
