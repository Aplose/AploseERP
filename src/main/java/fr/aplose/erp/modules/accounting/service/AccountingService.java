package fr.aplose.erp.modules.accounting.service;

import fr.aplose.erp.modules.accounting.entity.AccountingAccount;
import fr.aplose.erp.modules.accounting.entity.AccountingEntry;
import fr.aplose.erp.modules.accounting.entity.AccountingEntryLine;
import fr.aplose.erp.modules.accounting.entity.AccountingJournal;
import fr.aplose.erp.modules.accounting.repository.AccountingAccountRepository;
import fr.aplose.erp.modules.accounting.repository.AccountingEntryLineRepository;
import fr.aplose.erp.modules.accounting.repository.AccountingEntryRepository;
import fr.aplose.erp.modules.accounting.repository.AccountingJournalRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingService {

    private final AccountingAccountRepository accountRepository;
    private final AccountingJournalRepository journalRepository;
    private final AccountingEntryRepository entryRepository;
    private final AccountingEntryLineRepository entryLineRepository;

    @Transactional(readOnly = true)
    public List<AccountingJournal> findAllJournals() {
        return journalRepository.findByTenantIdAndActiveTrueOrderByCodeAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public AccountingJournal findJournalById(Long id) {
        return journalRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Journal not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AccountingAccount> findAllAccounts() {
        return accountRepository.findByTenantIdOrderByCodeAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<AccountingAccount> findActiveAccounts() {
        return accountRepository.findByTenantIdAndActiveTrueOrderByCodeAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public AccountingAccount findAccountById(Long id) {
        return accountRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<AccountingEntry> findEntries(Pageable pageable) {
        return entryRepository.findByTenantIdOrderByEntryDateDesc(TenantContext.getCurrentTenantId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<AccountingEntry> findEntriesByJournal(Long journalId, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        findJournalById(journalId);
        return entryRepository.findByTenantIdAndJournalIdOrderByEntryDateDesc(tid, journalId, pageable);
    }

    @Transactional(readOnly = true)
    public AccountingEntry findEntryById(Long id) {
        return entryRepository.findByIdAndTenantIdWithLines(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + id));
    }

    @Transactional
    public AccountingEntry saveEntry(AccountingEntry entry, Long currentUserId) {
        entry.setCreatedById(currentUserId);
        findJournalById(entry.getJournal().getId());
        for (AccountingEntryLine line : entry.getLines()) {
            line.setTenantId(TenantContext.getCurrentTenantId());
            line.setEntry(entry);
            findAccountById(line.getAccount().getId());
        }
        return entryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId, LocalDate from, LocalDate to) {
        findAccountById(accountId);
        BigDecimal sum = entryLineRepository.sumBalanceByTenantAndAccountAndDateRange(
                TenantContext.getCurrentTenantId(), accountId, from, to);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
