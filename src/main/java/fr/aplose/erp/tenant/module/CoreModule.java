package fr.aplose.erp.tenant.module;

import lombok.Getter;

/**
 * Registry of core (built-in) modules that can be enabled/disabled per tenant.
 * Each module corresponds to a major feature and a sidebar menu entry.
 */
@Getter
public enum CoreModule {

    CRM_THIRD_PARTY("CRM_THIRD_PARTY", "module.crm.thirdParty", "bi-building", "THIRD_PARTY_READ", "/third-parties", 10, "crm"),
    CRM_CONTACT("CRM_CONTACT", "module.crm.contact", "bi-person-lines-fill", "CONTACT_READ", "/contacts", 20, "crm"),
    COMMERCE_PROPOSAL("COMMERCE_PROPOSAL", "module.commerce.proposal", "bi-file-earmark-text", "PROPOSAL_READ", "/proposals", 30, "commerce"),
    COMMERCE_ORDER("COMMERCE_ORDER", "module.commerce.order", "bi-cart3", "SALES_ORDER_READ", "/orders", 40, "commerce"),
    COMMERCE_INVOICE("COMMERCE_INVOICE", "module.commerce.invoice", "bi-receipt", "INVOICE_READ", "/invoices", 50, "commerce"),
    CATALOG_PRODUCT("CATALOG_PRODUCT", "module.catalog.product", "bi-box-seam", "PRODUCT_READ", "/products", 60, "catalog"),
    PROJECT("PROJECT", "module.project", "bi-kanban", "PROJECT_READ", "/projects", 70, "project"),
    AGENDA("AGENDA", "module.agenda", "bi-calendar3", "AGENDA_READ", "/agenda", 80, "project"),
    BANK("BANK", "module.bank", "bi-bank", "BANK_READ", "/bank", 90, "bank"),
    ACCOUNTING("ACCOUNTING", "module.accounting", "bi-journal-bookmark", "ACCOUNTING_READ", "/accounting", 100, "accounting"),
    HR("HR", "module.hr", "bi-people", "HR_READ", "/hr", 110, "hr"),
    LEAVE_REQUEST("LEAVE_REQUEST", "module.leave", "bi-calendar-x", "LEAVE_READ", "/leave-requests", 115, "hr"),
    GED("GED", "module.ged", "bi-folder2-open", "GED_READ", "/ged", 120, "ged"),
    TICKETING("TICKETING", "module.ticketing", "bi-ticket-perforated", "TICKETING_READ", "/ticketing", 130, "ticketing"),
    REPORTING("REPORTING", "module.reporting", "bi-graph-up", "REPORT_READ", "/reporting", 140, "reporting");

    private final String code;
    private final String labelKey;
    private final String icon;
    private final String permissionRead;
    private final String menuPath;
    private final int sortOrder;
    private final String menuGroupId;

    CoreModule(String code, String labelKey, String icon, String permissionRead, String menuPath, int sortOrder, String menuGroupId) {
        this.code = code;
        this.labelKey = labelKey;
        this.icon = icon;
        this.permissionRead = permissionRead;
        this.menuPath = menuPath;
        this.sortOrder = sortOrder;
        this.menuGroupId = menuGroupId;
    }

    public static CoreModule fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (CoreModule m : values()) {
            if (m.getCode().equals(code)) return m;
        }
        return null;
    }

    public static CoreModule[] ordered() {
        CoreModule[] arr = values();
        java.util.Arrays.sort(arr, java.util.Comparator.comparingInt(CoreModule::getSortOrder));
        return arr;
    }
}
