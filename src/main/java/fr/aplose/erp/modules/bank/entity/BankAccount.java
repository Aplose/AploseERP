package fr.aplose.erp.modules.bank.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
public class BankAccount extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "bic", length = 11)
    private String bic;

    @JdbcTypeCode(SqlTypes.CHAR) // CHAR(n) in the schema

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode = "EUR";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
