package fr.aplose.erp.modules.extrafield.service;

import fr.aplose.erp.modules.extrafield.entity.ExtraFieldDefinition;
import fr.aplose.erp.modules.extrafield.entity.ExtraFieldValue;
import fr.aplose.erp.modules.extrafield.repository.ExtraFieldDefinitionRepository;
import fr.aplose.erp.modules.extrafield.repository.ExtraFieldValueRepository;
import fr.aplose.erp.modules.extrafield.web.dto.ExtraFieldDefinitionDto;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExtraFieldService {

    private final ExtraFieldDefinitionRepository defRepo;
    private final ExtraFieldValueRepository valRepo;

    @Transactional(readOnly = true)
    public List<ExtraFieldDefinition> getDefinitions(String entityType) {
        return defRepo.findByTenantIdAndEntityTypeOrderBySortOrderAsc(
                TenantContext.getCurrentTenantId(), entityType);
    }

    @Transactional(readOnly = true)
    public List<ExtraFieldDefinition> getActiveDefinitions(String entityType) {
        return defRepo.findByTenantIdAndEntityTypeAndActiveTrueOrderBySortOrderAsc(
                TenantContext.getCurrentTenantId(), entityType);
    }

    @Transactional(readOnly = true)
    public ExtraFieldDefinition getDefinitionById(Long id) {
        return defRepo.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Extra field not found: " + id));
    }

    @Transactional(readOnly = true)
    public Map<String, String> getValues(String entityType, Long entityId) {
        List<ExtraFieldValue> values = valRepo.findByTenantIdAndEntityTypeAndEntityId(
                TenantContext.getCurrentTenantId(), entityType, entityId);
        Map<String, String> map = new LinkedHashMap<>();
        for (ExtraFieldValue v : values) {
            map.put(v.getFieldCode(), v.getValueText());
        }
        return map;
    }

    @Transactional
    public void saveValues(String entityType, Long entityId, Map<String, String> values) {
        String tid = TenantContext.getCurrentTenantId();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String fieldCode = entry.getKey();
            String value = entry.getValue();

            ExtraFieldValue efv = valRepo.findByTenantIdAndEntityTypeAndEntityIdAndFieldCode(
                    tid, entityType, entityId, fieldCode)
                    .orElseGet(() -> {
                        ExtraFieldValue nv = new ExtraFieldValue();
                        nv.setTenantId(tid);
                        nv.setEntityType(entityType);
                        nv.setEntityId(entityId);
                        nv.setFieldCode(fieldCode);
                        return nv;
                    });
            efv.setValueText(value);
            valRepo.save(efv);
        }
    }

    @Transactional
    public ExtraFieldDefinition createDefinition(String entityType, ExtraFieldDefinitionDto dto) {
        String tid = TenantContext.getCurrentTenantId();
        defRepo.findByTenantIdAndEntityTypeAndFieldCode(tid, entityType, dto.getFieldCode())
                .ifPresent(d -> { throw new IllegalStateException("Field code already exists: " + dto.getFieldCode()); });

        ExtraFieldDefinition def = new ExtraFieldDefinition();
        def.setTenantId(tid);
        def.setEntityType(entityType);
        applyDto(def, dto);
        return defRepo.save(def);
    }

    @Transactional
    public ExtraFieldDefinition updateDefinition(Long id, ExtraFieldDefinitionDto dto) {
        ExtraFieldDefinition def = getDefinitionById(id);
        String tid = TenantContext.getCurrentTenantId();
        defRepo.findByTenantIdAndEntityTypeAndFieldCode(tid, def.getEntityType(), dto.getFieldCode())
                .filter(d -> !d.getId().equals(id))
                .ifPresent(d -> { throw new IllegalStateException("Field code already exists: " + dto.getFieldCode()); });
        applyDto(def, dto);
        return defRepo.save(def);
    }

    @Transactional
    public void deleteDefinition(Long id) {
        ExtraFieldDefinition def = getDefinitionById(id);
        defRepo.delete(def);
    }

    private void applyDto(ExtraFieldDefinition def, ExtraFieldDefinitionDto dto) {
        def.setFieldCode(dto.getFieldCode().trim().toLowerCase());
        def.setLabel(dto.getLabel());
        def.setFieldType(dto.getFieldType());
        def.setFieldOptions(dto.getFieldOptions());
        def.setDefaultValue(dto.getDefaultValue());
        def.setRequired(dto.isRequired());
        def.setSortOrder(dto.getSortOrder());
        def.setVisibleOnList(dto.isVisibleOnList());
        def.setVisibleOnDetail(dto.isVisibleOnDetail());
        def.setVisibleOnForm(dto.isVisibleOnForm());
        def.setActive(dto.isActive());
    }
}
