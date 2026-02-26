package fr.aplose.erp.modules.automation.service;

import fr.aplose.erp.modules.automation.entity.AutomationRule;
import fr.aplose.erp.modules.automation.repository.AutomationRuleRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationRuleService {

    private final AutomationRuleRepository repository;

    @Transactional(readOnly = true)
    public List<AutomationRule> findAll() {
        return repository.findByTenantIdOrderByNameAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public AutomationRule findById(Long id) {
        return repository.findById(id)
                .filter(r -> r.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
    }

    @Transactional
    public AutomationRule save(AutomationRule rule) {
        if (rule.getTenantId() == null || rule.getTenantId().isBlank()) {
            rule.setTenantId(TenantContext.getCurrentTenantId());
        }
        return repository.save(rule);
    }

    @Transactional
    public void deleteById(Long id) {
        AutomationRule rule = findById(id);
        repository.delete(rule);
    }

    /**
     * Run all enabled rules matching the trigger. Context should contain at least:
     * status, amount (BigDecimal), thirdPartyId (Long), entityId (Long) depending on trigger entity.
     */
    @Transactional(readOnly = true)
    public void runRules(String triggerEntity, String triggerEvent, Map<String, Object> context) {
        String tenantId = TenantContext.getCurrentTenantId();
        List<AutomationRule> rules = repository.findByTenantIdAndTriggerEntityAndTriggerEventAndEnabledTrue(
                tenantId, triggerEntity, triggerEvent);
        for (AutomationRule rule : rules) {
            if (matchesConditions(rule, context)) {
                try {
                    executeAction(rule, context);
                } catch (Exception e) {
                    log.warn("Automation rule {} failed: {}", rule.getId(), e.getMessage());
                }
            }
        }
    }

    private boolean matchesConditions(AutomationRule rule, Map<String, Object> context) {
        if (rule.getConditionStatus() != null && !rule.getConditionStatus().isBlank()) {
            Object status = context.get("status");
            if (status == null || !rule.getConditionStatus().equals(status.toString())) {
                return false;
            }
        }
        if (rule.getConditionAmountMin() != null) {
            Object amount = context.get("amount");
            if (amount == null || !(amount instanceof BigDecimal) ||
                    ((BigDecimal) amount).compareTo(rule.getConditionAmountMin()) < 0) {
                return false;
            }
        }
        if (rule.getConditionAmountMax() != null) {
            Object amount = context.get("amount");
            if (amount == null || !(amount instanceof BigDecimal) ||
                    ((BigDecimal) amount).compareTo(rule.getConditionAmountMax()) > 0) {
                return false;
            }
        }
        if (rule.getConditionThirdPartyId() != null) {
            Object tpId = context.get("thirdPartyId");
            if (tpId == null || !rule.getConditionThirdPartyId().equals(tpId instanceof Long ? (Long) tpId : Long.valueOf(tpId.toString()))) {
                return false;
            }
        }
        return true;
    }

    private void executeAction(AutomationRule rule, Map<String, Object> context) {
        switch (rule.getActionType() != null ? rule.getActionType().toUpperCase() : "") {
            case "LOG" -> log.info("Automation rule [{}] triggered: entity={}, event={}, context={}",
                    rule.getName(), rule.getTriggerEntity(), rule.getTriggerEvent(), context);
            case "SEND_EMAIL" -> {
                // Placeholder: in a full implementation, resolve template from actionParams and call MailService
                log.info("Automation rule [{}]: SEND_EMAIL would be sent (params={})", rule.getName(), rule.getActionParams());
            }
            default -> log.debug("Automation rule [{}]: unknown action type {}", rule.getName(), rule.getActionType());
        }
    }
}
