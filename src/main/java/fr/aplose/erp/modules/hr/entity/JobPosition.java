package fr.aplose.erp.modules.hr.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hr_job_positions")
@Getter
@Setter
@NoArgsConstructor
public class JobPosition extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
