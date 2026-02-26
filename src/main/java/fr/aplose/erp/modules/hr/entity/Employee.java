package fr.aplose.erp.modules.hr.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.security.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "hr_employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id")
    private JobPosition jobPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "ACTIVE";

    public String getDisplayName() {
        if (user != null) return user.getDisplayName();
        if (contact != null) {
            String fn = contact.getFirstName() != null ? contact.getFirstName() : "";
            String ln = contact.getLastName() != null ? contact.getLastName() : "";
            return (fn + " " + ln).trim();
        }
        return employeeNumber != null ? employeeNumber : "Employee #" + getId();
    }
}
