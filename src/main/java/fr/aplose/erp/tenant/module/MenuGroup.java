package fr.aplose.erp.tenant.module;

import lombok.Getter;

/**
 * Menu groups for the sidebar (expandable sections).
 * Order matches the plan: CRM, Commerce, Catalog, Project, Bank, Accounting, HR, GED, Ticketing, Reporting, Apps, Settings.
 */
@Getter
public enum MenuGroup {

    CRM("crm", "nav.group.crm", 10),
    COMMERCE("commerce", "nav.group.commerce", 20),
    CATALOG("catalog", "nav.group.catalog", 30),
    PROJECT("project", "nav.group.project", 40),
    BANK("bank", "nav.group.bank", 50),
    ACCOUNTING("accounting", "nav.group.accounting", 60),
    HR("hr", "nav.group.hr", 70),
    GED("ged", "nav.group.ged", 80),
    TICKETING("ticketing", "nav.group.ticketing", 90),
    REPORTING("reporting", "nav.group.reporting", 100),
    APPS("apps", "nav.group.apps", 110),
    SETTINGS("settings", "nav.group.settings", 120);

    private final String id;
    private final String labelKey;
    private final int sortOrder;

    MenuGroup(String id, String labelKey, int sortOrder) {
        this.id = id;
        this.labelKey = labelKey;
        this.sortOrder = sortOrder;
    }

    public static MenuGroup[] ordered() {
        MenuGroup[] arr = values();
        java.util.Arrays.sort(arr, java.util.Comparator.comparingInt(MenuGroup::getSortOrder));
        return arr;
    }
}
