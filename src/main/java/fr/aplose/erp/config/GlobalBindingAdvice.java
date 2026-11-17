package fr.aplose.erp.config;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Protects every form binding against mass assignment.
 * <p>
 * Several controllers bind JPA entities directly from the request (@ModelAttribute CrmActivity,
 * Ticket, Employee, PublicForm...). Without this, a user could add "id=123" to a creation form and
 * overwrite record 123 of another company, or send "tenantId=...". Identifiers always come from
 * the URL path (and are checked against the current company) or from explicitly named parameters.
 */
@ControllerAdvice
public class GlobalBindingAdvice {

    static final String[] PROTECTED_FIELDS = {
            "id", "tenantId", "createdAt", "updatedAt", "deletedAt", "createdById",
            "*.id", "*.tenantId"
    };

    @InitBinder
    public void protectTechnicalFields(WebDataBinder binder) {
        binder.setDisallowedFields(PROTECTED_FIELDS);
    }
}
