package fr.aplose.erp.modules.automation.entity;

import fr.aplose.erp.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "automation_rules")
@Getter
@Setter
@NoArgsConstructor
public class AutomationRule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "trigger_entity", nullable = false, length = 50)
    private String triggerEntity;

    @Column(name = "trigger_event", nullable = false, length = 50)
    private String triggerEvent;

    @Column(name = "condition_status", length = 50)
    private String conditionStatus;

    @Column(name = "condition_amount_min", precision = 19, scale = 4)
    private BigDecimal conditionAmountMin;

    @Column(name = "condition_amount_max", precision = 19, scale = 4)
    private BigDecimal conditionAmountMax;

    @Column(name = "condition_third_party_id")
    private Long conditionThirdPartyId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "action_params", columnDefinition = "TEXT")
    private String actionParams;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
