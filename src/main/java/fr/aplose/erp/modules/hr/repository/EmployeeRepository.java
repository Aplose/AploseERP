package fr.aplose.erp.modules.hr.repository;

import fr.aplose.erp.modules.hr.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<Employee> findByTenantIdAndManagerIdIsNullOrderByCreatedAtDesc(String tenantId);

    List<Employee> findByTenantIdAndManagerIdOrderByCreatedAtDesc(String tenantId, Long managerId);

    Optional<Employee> findByIdAndTenantId(Long id, String tenantId);

    Optional<Employee> findByTenantIdAndUserId(String tenantId, Long userId);
}
