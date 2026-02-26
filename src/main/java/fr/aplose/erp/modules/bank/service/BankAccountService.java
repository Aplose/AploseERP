package fr.aplose.erp.modules.bank.service;

import fr.aplose.erp.modules.bank.entity.BankAccount;
import fr.aplose.erp.modules.bank.entity.BankMovement;
import fr.aplose.erp.modules.bank.repository.BankAccountRepository;
import fr.aplose.erp.modules.bank.repository.BankMovementRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository accountRepository;
    private final BankMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public List<BankAccount> findAllActiveAccounts() {
        return accountRepository.findByTenantIdAndActiveTrueOrderByNameAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<BankAccount> findAllAccounts() {
        return accountRepository.findByTenantIdOrderByNameAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public BankAccount findAccountById(Long id) {
        return accountRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + id));
    }

    @Transactional
    public BankAccount saveAccount(BankAccount account) {
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Page<BankMovement> findMovements(Pageable pageable) {
        return movementRepository.findByTenantIdOrderByMovementDateDesc(TenantContext.getCurrentTenantId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<BankMovement> findMovementsByAccount(Long accountId, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        findAccountById(accountId); // ensure account belongs to tenant
        return movementRepository.findByTenantIdAndAccountIdOrderByMovementDateDesc(tid, accountId, pageable);
    }

    @Transactional(readOnly = true)
    public BankMovement findMovementById(Long id) {
        return movementRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Bank movement not found: " + id));
    }

    @Transactional
    public BankMovement saveMovement(BankMovement movement, Long currentUserId) {
        movement.setTenantId(TenantContext.getCurrentTenantId());
        movement.setCreatedById(currentUserId);
        findAccountById(movement.getAccount().getId()); // ensure account belongs to tenant
        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId) {
        String tid = TenantContext.getCurrentTenantId();
        findAccountById(accountId);
        BigDecimal sum = movementRepository.sumAmountByTenantIdAndAccountId(tid, accountId);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
