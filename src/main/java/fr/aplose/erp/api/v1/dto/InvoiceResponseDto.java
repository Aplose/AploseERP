package fr.aplose.erp.api.v1.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class InvoiceResponseDto {
    private Long id;
    private String reference;
    private String type;
    private Long thirdPartyId;
    private String status;
    private LocalDate dateIssued;
    private LocalDate dateDue;
    private String currencyCode;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private LocalDateTime createdAt;
    private List<InvoiceLineResponseDto> lines = new ArrayList<>();
}
