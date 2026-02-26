package fr.aplose.erp.modules.accounting.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounting_accounts")
@Getter
@Setter
@NoArgsConstructor
public class AccountingAccount extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "account_type", length = 30, nullable = false)
    private String accountType = "GENERAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AccountingAccount parent;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
