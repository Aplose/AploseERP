package fr.aplose.erp.modules.accounting.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounting_entries")
@Getter
@Setter
@NoArgsConstructor
public class AccountingEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private AccountingJournal journal;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "created_by")
    private Long createdById;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder, id")
    private List<AccountingEntryLine> lines = new ArrayList<>();
}
