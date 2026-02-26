package fr.aplose.erp.api.v1.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThirdPartyResponseDto {
    private Long id;
    private String code;
    private String name;
    private String type;
    private boolean customer;
    private boolean supplier;
    private boolean prospect;
    private String legalForm;
    private String taxId;
    private String email;
    private String phone;
    private String addressLine1;
    private String city;
    private String postalCode;
    private String countryCode;
    private String status;
    private LocalDateTime createdAt;
}
