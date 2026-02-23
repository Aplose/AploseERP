package fr.aplose.erp.modules.nocode.service;

import fr.aplose.erp.modules.nocode.entity.ModuleDefinition;
import fr.aplose.erp.modules.nocode.repository.ModuleDefinitionRepository;
import fr.aplose.erp.tenant.entity.TenantModule;
import fr.aplose.erp.tenant.repository.TenantModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModuleDefinitionService {

    private final ModuleDefinitionRepository moduleDefinitionRepository;
    private final TenantModuleRepository tenantModuleRepository;

    public List<ModuleDefinition> findAllPublic() {
        return moduleDefinitionRepository.findByIsPublicTrueOrderByNameAsc();
    }

    public Optional<ModuleDefinition> findById(Long id) {
        return moduleDefinitionRepository.findById(id);
    }

    /**
     * Activates a no-code module for the tenant: creates a TenantModule linking to this definition.
     */
    @Transactional
    public void activateForTenant(String tenantId, Long moduleDefinitionId) {
        if (tenantId == null || moduleDefinitionId == null) return;
        ModuleDefinition md = moduleDefinitionRepository.findById(moduleDefinitionId).orElse(null);
        if (md == null) return;
        String moduleCode = "module:" + md.getCode();
        if (tenantModuleRepository.findByTenantIdAndModuleCode(tenantId, moduleCode).isPresent()) {
            return; // already activated
        }
        TenantModule tm = new TenantModule();
        tm.setTenantId(tenantId);
        tm.setModuleCode(moduleCode);
        tm.setEnabled(true);
        tm.setModuleDefinition(md);
        tenantModuleRepository.save(tm);
    }

    public boolean isActivatedForTenant(String tenantId, Long moduleDefinitionId) {
        if (tenantId == null || moduleDefinitionId == null) return false;
        return tenantModuleRepository.findByTenantIdAndModuleDefinition_Id(tenantId, moduleDefinitionId).isPresent();
    }
}
