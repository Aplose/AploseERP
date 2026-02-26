package fr.aplose.erp.modules.accounting.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounting_journals")
@Getter
@Setter
@NoArgsConstructor
public class AccountingJournal extends BaseEntity {

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
