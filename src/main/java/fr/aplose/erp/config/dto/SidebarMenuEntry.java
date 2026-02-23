package fr.aplose.erp.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single menu entry (link) within a sidebar section.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SidebarMenuEntry {

    private String path;
    private String labelKey;   // i18n key; null when using label (e.g. no-code)
    private String label;      // raw label when labelKey is null (e.g. no-code entity name)
    private String icon;
    private String permission;
    private String moduleCode; // null for non-module entries (e.g. admin links)
    private boolean active;    // true if requestURI starts with path
}
