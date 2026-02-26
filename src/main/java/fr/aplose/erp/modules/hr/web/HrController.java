package fr.aplose.erp.modules.hr.web;

import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.hr.entity.Contract;
import fr.aplose.erp.modules.hr.entity.Employee;
import fr.aplose.erp.modules.hr.entity.JobPosition;
import fr.aplose.erp.modules.hr.service.HrService;
import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('HR_READ')")
    public String index(Model model) {
        model.addAttribute("employeesCount", hrService.findAllEmployees().size());
        model.addAttribute("positionsCount", hrService.findAllPositions().size());
        model.addAttribute("contractsCount", hrService.findAllContracts().size());
        return "modules/hr/index";
    }

    // --- Employees ---
    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('HR_READ')")
    public String employeeList(Model model) {
        model.addAttribute("employees", hrService.findAllEmployees());
        return "modules/hr/employee-list";
    }

    @GetMapping("/employees/new")
    @PreAuthorize("hasAuthority('HR_CREATE')")
    public String newEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        addEmployeeFormRefs(model);
        return "modules/hr/employee-form";
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAuthority('HR_CREATE')")
    public String createEmployee(@ModelAttribute @Valid Employee employee,
                                 @RequestParam(required = false) Long userId,
                                 @RequestParam(required = false) Long contactId,
                                 @RequestParam(required = false) Long jobPositionId,
                                 @RequestParam(required = false) Long managerId,
                                 BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            addEmployeeFormRefs(model);
            return "modules/hr/employee-form";
        }
        setEmployeeRefs(employee, userId, contactId, jobPositionId, managerId);
        hrService.saveEmployee(employee);
        ra.addFlashAttribute("message", "Employee created.");
        return "redirect:/hr/employees";
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('HR_READ')")
    public String employeeDetail(@PathVariable Long id, Model model) {
        Employee employee = hrService.getEmployeeById(id);
        if (employee == null) return "redirect:/hr/employees";
        model.addAttribute("employee", employee);
        model.addAttribute("contracts", hrService.findAllContracts().stream()
                .filter(c -> c.getEmployee().getId().equals(id)).toList());
        return "modules/hr/employee-detail";
    }

    @GetMapping("/employees/{id}/edit")
    @PreAuthorize("hasAuthority('HR_UPDATE')")
    public String editEmployeeForm(@PathVariable Long id, Model model) {
        Employee employee = hrService.getEmployeeById(id);
        if (employee == null) return "redirect:/hr/employees";
        model.addAttribute("employee", employee);
        addEmployeeFormRefs(model);
        return "modules/hr/employee-form";
    }

    @PostMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('HR_UPDATE')")
    public String updateEmployee(@PathVariable Long id,
                                 @ModelAttribute @Valid Employee employee,
                                 @RequestParam(required = false) Long userId,
                                 @RequestParam(required = false) Long contactId,
                                 @RequestParam(required = false) Long jobPositionId,
                                 @RequestParam(required = false) Long managerId,
                                 BindingResult result, Model model, RedirectAttributes ra) {
        Employee existing = hrService.getEmployeeById(id);
        if (existing == null) return "redirect:/hr/employees";
        if (result.hasErrors()) {
            employee.setId(id);
            model.addAttribute("employee", employee);
            addEmployeeFormRefs(model);
            return "modules/hr/employee-form";
        }
        employee.setId(id);
        employee.setTenantId(existing.getTenantId());
        employee.setCreatedAt(existing.getCreatedAt());
        setEmployeeRefs(employee, userId, contactId, jobPositionId, managerId);
        hrService.saveEmployee(employee);
        ra.addFlashAttribute("message", "Employee updated.");
        return "redirect:/hr/employees/" + id;
    }

    @PostMapping("/employees/{id}/delete")
    @PreAuthorize("hasAuthority('HR_DELETE')")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes ra) {
        hrService.deleteEmployee(id);
        ra.addFlashAttribute("message", "Employee deleted.");
        return "redirect:/hr/employees";
    }

    // --- Job positions ---
    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('HR_READ')")
    public String positionList(Model model) {
        model.addAttribute("positions", hrService.findAllPositions());
        return "modules/hr/position-list";
    }

    @GetMapping("/positions/new")
    @PreAuthorize("hasAuthority('HR_CREATE')")
    public String newPositionForm(Model model) {
        model.addAttribute("position", new JobPosition());
        return "modules/hr/position-form";
    }

    @PostMapping("/positions")
    @PreAuthorize("hasAuthority('HR_CREATE')")
    public String createPosition(@ModelAttribute @Valid JobPosition position, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return "modules/hr/position-form";
        hrService.savePosition(position);
        ra.addFlashAttribute("message", "Position created.");
        return "redirect:/hr/positions";
    }

    @GetMapping("/positions/{id}/edit")
    @PreAuthorize("hasAuthority('HR_UPDATE')")
    public String editPositionForm(@PathVariable Long id, Model model) {
        JobPosition position = hrService.getPositionById(id);
        if (position == null) return "redirect:/hr/positions";
        model.addAttribute("position", position);
        return "modules/hr/position-form";
    }

    @PostMapping("/positions/{id}")
    @PreAuthorize("hasAuthority('HR_UPDATE')")
    public String updatePosition(@PathVariable Long id, @ModelAttribute @Valid JobPosition position, BindingResult result, Model model, RedirectAttributes ra) {
        JobPosition existing = hrService.getPositionById(id);
        if (existing == null) return "redirect:/hr/positions";
        if (result.hasErrors()) {
            position.setId(id);
            model.addAttribute("position", position);
            return "modules/hr/position-form";
        }
        position.setId(id);
        position.setTenantId(existing.getTenantId());
        position.setCreatedAt(existing.getCreatedAt());
        hrService.savePosition(position);
        ra.addFlashAttribute("message", "Position updated.");
        return "redirect:/hr/positions";
    }

    @PostMapping("/positions/{id}/delete")
    @PreAuthorize("hasAuthority('HR_DELETE')")
    public String deletePosition(@PathVariable Long id, RedirectAttributes ra) {
        hrService.deletePosition(id);
        ra.addFlashAttribute("message", "Position deleted.");
        return "redirect:/hr/positions";
    }

    // --- Contracts ---
    @GetMapping("/contracts")
    @PreAuthorize("hasAuthority('HR_READ')")
    public String contractList(Model model) {
        model.addAttribute("contracts", hrService.findAllContracts());
        return "modules/hr/contract-list";
    }

    @GetMapping("/contracts/new")
    @PreAuthorize("hasAuthority('HR_CREATE')")
    public String newContractForm(Model model, @RequestParam(required = false) Long employeeId) {
        Contract contract = new Contract();
        if (employeeId != null) {
            Employee emp = hrService.getEmployeeById(employeeId);
            if (emp != null) contract.setEmployee(emp);
        }
        model.addAttribute("contract", contract);
        model.addAttribute("employees", hrService.findAllEmployees());
        return "modules/hr/contract-form";
    }

    @PostMapping("/contracts")
    @PreAuthorize("hasAuthority('HR_CREATE')")
    public String createContract(@ModelAttribute @Valid Contract contract,
                                @RequestParam(required = false) Long employeeId,
                                BindingResult result, Model model, RedirectAttributes ra) {
        if (employeeId == null) {
            result.rejectValue("employee", "required", "Employee is required");
        }
        if (result.hasErrors()) {
            model.addAttribute("employees", hrService.findAllEmployees());
            return "modules/hr/contract-form";
        }
        Employee emp = hrService.getEmployeeById(employeeId);
        if (emp == null) {
            model.addAttribute("employees", hrService.findAllEmployees());
            result.rejectValue("employee", "required", "Employee is required");
            return "modules/hr/contract-form";
        }
        contract.setEmployee(emp);
        hrService.saveContract(contract);
        ra.addFlashAttribute("message", "Contract created.");
        return "redirect:/hr/contracts";
    }

    @GetMapping("/contracts/{id}/edit")
    @PreAuthorize("hasAuthority('HR_UPDATE')")
    public String editContractForm(@PathVariable Long id, Model model) {
        Contract contract = hrService.getContractById(id);
        if (contract == null) return "redirect:/hr/contracts";
        model.addAttribute("contract", contract);
        model.addAttribute("employees", hrService.findAllEmployees());
        return "modules/hr/contract-form";
    }

    @PostMapping("/contracts/{id}")
    @PreAuthorize("hasAuthority('HR_UPDATE')")
    public String updateContract(@PathVariable Long id,
                                 @ModelAttribute @Valid Contract contract,
                                 @RequestParam Long employeeId,
                                 BindingResult result, Model model, RedirectAttributes ra) {
        Contract existing = hrService.getContractById(id);
        if (existing == null) return "redirect:/hr/contracts";
        if (result.hasErrors()) {
            contract.setId(id);
            model.addAttribute("contract", contract);
            model.addAttribute("employees", hrService.findAllEmployees());
            return "modules/hr/contract-form";
        }
        contract.setId(id);
        contract.setTenantId(existing.getTenantId());
        contract.setCreatedAt(existing.getCreatedAt());
        Employee emp = hrService.getEmployeeById(employeeId);
        if (emp != null) contract.setEmployee(emp);
        hrService.saveContract(contract);
        ra.addFlashAttribute("message", "Contract updated.");
        return "redirect:/hr/contracts";
    }

    @PostMapping("/contracts/{id}/delete")
    @PreAuthorize("hasAuthority('HR_DELETE')")
    public String deleteContract(@PathVariable Long id, RedirectAttributes ra) {
        hrService.deleteContract(id);
        ra.addFlashAttribute("message", "Contract deleted.");
        return "redirect:/hr/contracts";
    }

    // --- Organigramme ---
    @GetMapping("/organigramme")
    @PreAuthorize("hasAuthority('HR_READ')")
    public String organigramme(Model model) {
        model.addAttribute("roots", hrService.findRootEmployeesForOrganigramme());
        model.addAttribute("hrService", hrService);
        return "modules/hr/organigramme";
    }

    private void addEmployeeFormRefs(Model model) {
        String tid = TenantContext.getCurrentTenantId();
        model.addAttribute("users", userRepository.findByTenantIdAndDeletedAtIsNull(tid, PageRequest.of(0, 500, Sort.by("lastName"))).getContent());
        model.addAttribute("contacts", contactRepository.findByTenantIdAndDeletedAtIsNull(tid, PageRequest.of(0, 500, Sort.by("lastName"))).getContent());
        model.addAttribute("positions", hrService.findAllPositions());
        model.addAttribute("employees", hrService.findAllEmployees());
    }

    private void setEmployeeRefs(Employee employee, Long userId, Long contactId, Long jobPositionId, Long managerId) {
        if (userId != null) employee.setUser(userRepository.getReferenceById(userId));
        else employee.setUser(null);
        if (contactId != null) employee.setContact(contactRepository.getReferenceById(contactId));
        else employee.setContact(null);
        if (jobPositionId != null) employee.setJobPosition(hrService.getPositionById(jobPositionId));
        else employee.setJobPosition(null);
        if (managerId != null) employee.setManager(hrService.getEmployeeById(managerId));
        else employee.setManager(null);
    }
}
