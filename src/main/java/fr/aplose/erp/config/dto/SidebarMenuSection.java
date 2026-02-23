package fr.aplose.erp.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Expandable sidebar section (group) with its entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SidebarMenuSection {

    private String id;
    private String labelKey;
    private List<SidebarMenuEntry> entries;
    private boolean expandedByDefault; // true if any entry is active
}
