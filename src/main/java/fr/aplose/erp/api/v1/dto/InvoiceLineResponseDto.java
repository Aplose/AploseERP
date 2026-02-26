package fr.aplose.erp.api.v1.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceLineResponseDto {
    private Long id;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
