package fr.aplose.erp.modules.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "accounting_entry_lines")
@Getter
@Setter
@NoArgsConstructor
public class AccountingEntryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private AccountingEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountingAccount account;

    @Column(name = "debit", precision = 19, scale = 4, nullable = false)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "credit", precision = 19, scale = 4, nullable = false)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "line_description", length = 500)
    private String lineDescription;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
