package fr.aplose.erp.modules.hr.service;

import fr.aplose.erp.modules.hr.entity.Contract;
import fr.aplose.erp.modules.hr.entity.Employee;
import fr.aplose.erp.modules.hr.entity.JobPosition;
import fr.aplose.erp.modules.hr.repository.ContractRepository;
import fr.aplose.erp.modules.hr.repository.EmployeeRepository;
import fr.aplose.erp.modules.hr.repository.JobPositionRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrService {

    private final JobPositionRepository jobPositionRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;

    @Transactional(readOnly = true)
    public List<JobPosition> findAllPositions() {
        return jobPositionRepository.findByTenantIdOrderByCode(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<Employee> findAllEmployees() {
        return employeeRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<Employee> findRootEmployeesForOrganigramme() {
        return employeeRepository.findByTenantIdAndManagerIdIsNullOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<Employee> findSubordinates(Long managerId) {
        return employeeRepository.findByTenantIdAndManagerIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId(), managerId);
    }

    @Transactional(readOnly = true)
    public List<Contract> findAllContracts() {
        return contractRepository.findByTenantIdOrderByStartDateDesc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public JobPosition getPositionById(Long id) {
        return jobPositionRepository.findById(id)
                .filter(p -> p.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public Contract getContractById(Long id) {
        return contractRepository.findById(id)
                .filter(c -> c.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .orElse(null);
    }

    @Transactional
    public JobPosition savePosition(JobPosition position) {
        return jobPositionRepository.save(position);
    }

    @Transactional
    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Transactional
    public Contract saveContract(Contract contract) {
        return contractRepository.save(contract);
    }

    @Transactional
    public void deletePosition(Long id) {
        jobPositionRepository.findById(id)
                .filter(p -> p.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .ifPresent(jobPositionRepository::delete);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employeeRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .ifPresent(employeeRepository::delete);
    }

    @Transactional
    public void deleteContract(Long id) {
        contractRepository.findById(id)
                .filter(c -> c.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .ifPresent(contractRepository::delete);
    }
}
