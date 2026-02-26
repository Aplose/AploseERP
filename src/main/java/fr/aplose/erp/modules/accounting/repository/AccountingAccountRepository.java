package fr.aplose.erp.modules.accounting.repository;

import fr.aplose.erp.modules.accounting.entity.AccountingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingAccountRepository extends JpaRepository<AccountingAccount, Long> {

    List<AccountingAccount> findByTenantIdAndActiveTrueOrderByCodeAsc(String tenantId);

    List<AccountingAccount> findByTenantIdOrderByCodeAsc(String tenantId);

    Optional<AccountingAccount> findByIdAndTenantId(Long id, String tenantId);

    Optional<AccountingAccount> findByTenantIdAndCode(String tenantId, String code);
}
