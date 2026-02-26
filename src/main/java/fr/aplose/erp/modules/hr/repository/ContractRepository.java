package fr.aplose.erp.modules.hr.repository;

import fr.aplose.erp.modules.hr.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByTenantIdOrderByStartDateDesc(String tenantId);

    List<Contract> findByEmployeeIdOrderByStartDateDesc(Long employeeId);
}
