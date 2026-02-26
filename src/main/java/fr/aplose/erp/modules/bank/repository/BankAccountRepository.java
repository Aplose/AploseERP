package fr.aplose.erp.modules.bank.repository;

import fr.aplose.erp.modules.bank.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findByTenantIdAndActiveTrueOrderByNameAsc(String tenantId);

    List<BankAccount> findByTenantIdOrderByNameAsc(String tenantId);

    Optional<BankAccount> findByIdAndTenantId(Long id, String tenantId);
}
