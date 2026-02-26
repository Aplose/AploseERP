package fr.aplose.erp.config;

import fr.aplose.erp.config.dto.SidebarMenuEntry;
import fr.aplose.erp.config.dto.SidebarMenuSection;
import fr.aplose.erp.modules.nocode.dto.NoCodeMenuEntry;
import fr.aplose.erp.tenant.module.CoreModule;
import fr.aplose.erp.tenant.module.MenuGroup;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds the sidebar menu sections (expandable groups) for the layout.
 * Sections are only included if they have at least one entry (module-enabled for core; non-empty for apps; always for settings).
 */
@Service
public class MenuService {

    /** Admin/Settings entries: path, labelKey, icon, permission (no moduleCode). */
    private static final List<SettingsEntryDef> SETTINGS_ENTRIES = List.of(
            new SettingsEntryDef("/admin/users", "nav.users", "bi-people", "USER_READ"),
            new SettingsEntryDef("/admin/roles", "nav.roles", "bi-shield-lock", "ROLE_READ"),
            new SettingsEntryDef("/admin/settings", "nav.settings", "bi-gear", "TENANT_READ"),
            new SettingsEntryDef("/admin/modules", "nav.modules", "bi-puzzle", "TENANT_READ"),
            new SettingsEntryDef("/admin/module-catalogue", "nocode.catalogue.title", "bi-collection", "TENANT_READ"),
            new SettingsEntryDef("/admin/dictionaries", "nav.dictionaries", "bi-journal-text", "TENANT_READ"),
            new SettingsEntryDef("/admin/extrafields", "nav.extrafields", "bi-sliders", "EXTRAFIELD_ADMIN"),
            new SettingsEntryDef("/admin/email-templates", "nav.emailTemplates", "bi-envelope", "ROLE_SUPER_ADMIN"),
            new SettingsEntryDef("/admin/api-keys", "apikey.title", "bi-key", "API_KEY_READ"),
            new SettingsEntryDef("/admin/webhooks", "webhook.title", "bi-link-45deg", "WEBHOOK_READ"),
            new SettingsEntryDef("/admin/public-forms", "publicform.admin.title", "bi-input-cursor", "PUBLIC_FORM_READ"),
            new SettingsEntryDef("/admin/ai", "ai.admin.title", "bi-stars", "AI_USE"),
            new SettingsEntryDef("/admin/dolibarr-import", "dolibarr.import.title", "bi-cloud-download", "DOLIBARR_IMPORT")
    );

    /**
     * Builds the list of sidebar sections in menu group order.
     * Only includes entries the user is allowed to see (permission check done here so sec:authorize is not needed in the template).
     *
     * @param userAuthorities set of authority strings (e.g. from Authentication.getAuthorities()); empty or null = no entries shown
     */
    public List<SidebarMenuSection> buildSidebarSections(
            String requestURI,
            Set<String> enabledModules,
            List<NoCodeMenuEntry> noCodeMenuEntries,
            Set<String> userAuthorities) {
        if (enabledModules == null) enabledModules = Set.of();
        if (noCodeMenuEntries == null) noCodeMenuEntries = List.of();
        if (userAuthorities == null) userAuthorities = Set.of();
        String uri = requestURI != null ? requestURI : "";

        List<SidebarMenuSection> sections = new ArrayList<>();
        for (MenuGroup group : MenuGroup.ordered()) {
            List<SidebarMenuEntry> entries = new ArrayList<>();

            if (group == MenuGroup.SETTINGS) {
                for (SettingsEntryDef def : SETTINGS_ENTRIES) {
                    if (!hasPermission(userAuthorities, def.permission)) continue;
                    entries.add(SidebarMenuEntry.builder()
                            .path(def.path)
                            .labelKey(def.labelKey)
                            .icon(def.icon)
                            .permission(def.permission)
                            .moduleCode(null)
                            .active(uri.startsWith(def.path))
                            .build());
                }
            } else if (group == MenuGroup.APPS) {
                if (!hasPermission(userAuthorities, "CUSTOM_ENTITY_READ")) {
                    // skip building apps entries
                } else {
                    for (NoCodeMenuEntry nce : noCodeMenuEntries) {
                        entries.add(SidebarMenuEntry.builder()
                                .path(nce.getPath())
                                .labelKey(null)
                                .label(nce.getLabel())
                                .icon(nce.getIcon() != null ? nce.getIcon() : "bi-box")
                                .permission("CUSTOM_ENTITY_READ")
                                .moduleCode(nce.getModuleCode())
                                .active(uri.startsWith(nce.getPath()))
                                .build());
                    }
                }
            } else {
                for (CoreModule core : CoreModule.ordered()) {
                    if (!group.getId().equals(core.getMenuGroupId()) || !enabledModules.contains(core.getCode())) {
                        continue;
                    }
                    if (!hasPermission(userAuthorities, core.getPermissionRead())) continue;
                    entries.add(SidebarMenuEntry.builder()
                            .path(core.getMenuPath())
                            .labelKey(core.getLabelKey())
                            .icon(core.getIcon())
                            .permission(core.getPermissionRead())
                            .moduleCode(core.getCode())
                            .active(uri.startsWith(core.getMenuPath()))
                            .build());
                }
            }

            if (entries.isEmpty()) continue;

            boolean expandedByDefault = entries.stream().anyMatch(SidebarMenuEntry::isActive);
            sections.add(SidebarMenuSection.builder()
                    .id(group.getId())
                    .labelKey(group.getLabelKey())
                    .entries(entries)
                    .expandedByDefault(expandedByDefault)
                    .build());
        }
        return sections;
    }

    /** Email-templates entry is visible with ROLE_SUPER_ADMIN or SUPER_ADMIN. */
    private boolean hasPermission(Set<String> userAuthorities, String requiredPermission) {
        if (requiredPermission == null) return true;
        if (userAuthorities.contains(requiredPermission)) return true;
        if ("ROLE_SUPER_ADMIN".equals(requiredPermission) && userAuthorities.contains("SUPER_ADMIN")) return true;
        return false;
    }

    private record SettingsEntryDef(String path, String labelKey, String icon, String permission) {}
}
