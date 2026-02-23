package fr.aplose.erp.tenant.service;

import fr.aplose.erp.modules.nocode.dto.NoCodeMenuEntry;
import fr.aplose.erp.modules.nocode.entity.CustomEntityDefinition;
import fr.aplose.erp.modules.nocode.entity.ModuleDefinition;
import fr.aplose.erp.tenant.entity.TenantModule;
import fr.aplose.erp.tenant.module.CoreModule;
import fr.aplose.erp.tenant.repository.TenantModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TenantModuleService {

    private final TenantModuleRepository tenantModuleRepository;

    /**
     * Returns the set of module codes enabled for the tenant.
     * If no row exists for a core module, it is considered enabled (default).
     */
    public Set<String> getEnabledModuleCodes(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return Set.of();
        List<TenantModule> all = tenantModuleRepository.findByTenantIdOrderByModuleCode(tenantId);
        Set<String> enabled = new HashSet<>();
        for (CoreModule core : CoreModule.values()) {
            boolean isEnabled = all.stream()
                    .filter(tm -> tm.getModuleCode().equals(core.getCode()))
                    .findFirst()
                    .map(TenantModule::isEnabled)
                    .orElse(true); // default enabled if no row
            if (isEnabled) enabled.add(core.getCode());
        }
        return enabled;
    }

    public boolean isModuleEnabled(String tenantId, String moduleCode) {
        if (tenantId == null || moduleCode == null) return false;
        return tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .map(TenantModule::isEnabled)
                .orElse(true); // no row = enabled by default
    }

    @Transactional
    public void updateModuleEnabled(String tenantId, String moduleCode, boolean enabled) {
        if (tenantId == null || moduleCode == null) return;
        if (CoreModule.fromCode(moduleCode) == null) return;
        TenantModule tm = tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .orElseGet(() -> {
                    TenantModule newTm = new TenantModule();
                    newTm.setTenantId(tenantId);
                    newTm.setModuleCode(moduleCode);
                    newTm.setEnabled(true);
                    return newTm;
                });
        tm.setEnabled(enabled);
        tenantModuleRepository.save(tm);
    }

    /**
     * Ensures the tenant has a row for every core module (default enabled).
     * Call this when creating a new tenant.
     */
    @Transactional
    public void ensureTenantHasModuleRows(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return;
        for (CoreModule core : CoreModule.values()) {
            if (!tenantModuleRepository.existsByTenantIdAndModuleCode(tenantId, core.getCode())) {
                TenantModule tm = new TenantModule();
                tm.setTenantId(tenantId);
                tm.setModuleCode(core.getCode());
                tm.setEnabled(true);
                tenantModuleRepository.save(tm);
            }
        }
    }

    /**
     * All core modules in display order (for admin UI).
     */
    public List<CoreModule> getAllCoreModules() {
        return List.of(CoreModule.ordered());
    }

    /**
     * Menu entries for no-code (custom) modules enabled for the tenant.
     */
    public List<NoCodeMenuEntry> getNoCodeMenuEntries(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return List.of();
        List<TenantModule> noCodeModules = tenantModuleRepository.findNoCodeEnabledByTenantId(tenantId);
        List<NoCodeMenuEntry> entries = new ArrayList<>();
        for (TenantModule tm : noCodeModules) {
            ModuleDefinition md = tm.getModuleDefinition();
            if (md == null) continue;
            String moduleCode = md.getCode();
            for (CustomEntityDefinition ced : md.getCustomEntityDefinitions()) {
                entries.add(new NoCodeMenuEntry(
                        moduleCode,
                        ced.getCode(),
                        ced.getName(),
                        "bi-box",
                        "/app/" + moduleCode + "/" + ced.getCode()
                ));
            }
        }
        return entries;
    }
}
