package fr.aplose.erp.core.businessobject;

import fr.aplose.erp.core.entity.BaseEntity;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Central registry of business object types (core and, in phase 3, custom/no-code).
 * Used for permissions, extra fields, menu, and future no-code extensions.
 */
public final class BusinessObjectRegistry {

    private BusinessObjectRegistry() {}

    @Getter
    public enum CoreType {
        THIRD_PARTY("THIRD_PARTY", "businessObject.thirdParty", "bi-building", "THIRD_PARTY_READ", "CRM_THIRD_PARTY", fr.aplose.erp.modules.thirdparty.entity.ThirdParty.class),
        CONTACT("CONTACT", "businessObject.contact", "bi-person-lines-fill", "CONTACT_READ", "CRM_CONTACT", fr.aplose.erp.modules.contact.entity.Contact.class),
        PRODUCT("PRODUCT", "businessObject.product", "bi-box-seam", "PRODUCT_READ", "CATALOG_PRODUCT", fr.aplose.erp.modules.catalog.entity.Product.class),
        PROPOSAL("PROPOSAL", "businessObject.proposal", "bi-file-earmark-text", "PROPOSAL_READ", "COMMERCE_PROPOSAL", fr.aplose.erp.modules.commerce.entity.Proposal.class),
        SALES_ORDER("SALES_ORDER", "businessObject.salesOrder", "bi-cart3", "SALES_ORDER_READ", "COMMERCE_ORDER", fr.aplose.erp.modules.commerce.entity.SalesOrder.class),
        INVOICE("INVOICE", "businessObject.invoice", "bi-receipt", "INVOICE_READ", "COMMERCE_INVOICE", fr.aplose.erp.modules.commerce.entity.Invoice.class),
        PAYMENT("PAYMENT", "businessObject.payment", "bi-cash", "PAYMENT_READ", "COMMERCE_INVOICE", fr.aplose.erp.modules.commerce.entity.Payment.class),
        PROJECT("PROJECT", "businessObject.project", "bi-kanban", "PROJECT_READ", "PROJECT", fr.aplose.erp.modules.project.entity.Project.class),
        PROJECT_TASK("PROJECT_TASK", "businessObject.projectTask", "bi-check2-square", "TASK_READ", "PROJECT", fr.aplose.erp.modules.project.entity.ProjectTask.class),
        AGENDA_EVENT("AGENDA_EVENT", "businessObject.agendaEvent", "bi-calendar3", "AGENDA_READ", "AGENDA", fr.aplose.erp.modules.agenda.entity.AgendaEvent.class);

        private final String code;
        private final String labelKey;
        private final String icon;
        private final String permissionRead;
        private final String moduleCode;
        private final Class<?> entityClass;

        CoreType(String code, String labelKey, String icon, String permissionRead, String moduleCode, Class<?> entityClass) {
            this.code = code;
            this.labelKey = labelKey;
            this.icon = icon;
            this.permissionRead = permissionRead;
            this.moduleCode = moduleCode;
            this.entityClass = entityClass;
        }
    }

    public static Optional<CoreType> getByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return Arrays.stream(CoreType.values())
                .filter(t -> t.getCode().equals(code))
                .findFirst();
    }

    public static List<CoreType> allCoreTypes() {
        return Arrays.asList(CoreType.values());
    }

    /**
     * Returns core types that have an entity class extending BaseEntity (for extra fields, etc.).
     */
    public static List<CoreType> coreTypesWithEntity() {
        return Arrays.stream(CoreType.values())
                .filter(t -> t.getEntityClass() != null && BaseEntity.class.isAssignableFrom(t.getEntityClass()))
                .collect(Collectors.toList());
    }
}
