package fr.aplose.erp.modules.fieldconfig.service;

import fr.aplose.erp.modules.fieldconfig.entity.FieldVisibilityConfig;
import fr.aplose.erp.modules.fieldconfig.repository.FieldVisibilityConfigRepository;
import fr.aplose.erp.modules.fieldconfig.web.dto.FieldVisibilityDto;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FieldConfigService {

    private final FieldVisibilityConfigRepository repo;

    @Transactional(readOnly = true)
    public Map<String, FieldVisibilityConfig> getConfig(String entityType) {
        String tid = TenantContext.getCurrentTenantId();
        List<FieldVisibilityConfig> configs = repo.findByTenantIdAndEntityType(tid, entityType);
        Map<String, FieldVisibilityConfig> map = new HashMap<>();
        for (FieldVisibilityConfig c : configs) {
            map.put(c.getFieldName(), c);
        }
        return map;
    }

    public boolean isVisible(Map<String, FieldVisibilityConfig> config, String fieldName, String viewType) {
        if (config == null || !config.containsKey(fieldName)) {
            return true;
        }
        FieldVisibilityConfig c = config.get(fieldName);
        return switch (viewType) {
            case "list" -> c.isVisibleOnList();
            case "detail" -> c.isVisibleOnDetail();
            case "form" -> c.isVisibleOnForm();
            default -> true;
        };
    }

    @Transactional
    public void saveConfig(String entityType, List<FieldVisibilityDto> dtos) {
        String tid = TenantContext.getCurrentTenantId();
        for (FieldVisibilityDto dto : dtos) {
            FieldVisibilityConfig config = repo.findByTenantIdAndEntityTypeAndFieldName(
                    tid, entityType, dto.getFieldName())
                    .orElseGet(() -> {
                        FieldVisibilityConfig nc = new FieldVisibilityConfig();
                        nc.setTenantId(tid);
                        nc.setEntityType(entityType);
                        nc.setFieldName(dto.getFieldName());
                        return nc;
                    });
            config.setVisibleOnList(dto.isVisibleOnList());
            config.setVisibleOnDetail(dto.isVisibleOnDetail());
            config.setVisibleOnForm(dto.isVisibleOnForm());
            config.setSortOrder(dto.getSortOrder());
            repo.save(config);
        }
    }
}
