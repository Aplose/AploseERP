package fr.aplose.erp.modules.nocode.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.aplose.erp.modules.nocode.entity.CustomEntityData;
import fr.aplose.erp.modules.nocode.entity.CustomEntityDefinition;
import fr.aplose.erp.modules.nocode.repository.CustomEntityDataRepository;
import fr.aplose.erp.modules.nocode.repository.CustomEntityDefinitionRepository;
import fr.aplose.erp.modules.nocode.repository.ModuleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomEntityService {

    private final CustomEntityDataRepository dataRepository;
    private final CustomEntityDefinitionRepository definitionRepository;
    private final ModuleDefinitionRepository moduleDefinitionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<CustomEntityDefinition> findDefinitionByModuleAndObjectCode(String moduleCode, String objectCode) {
        return moduleDefinitionRepository.findFirstByCodeOrderByUpdatedAtDesc(moduleCode)
                .flatMap(md -> definitionRepository.findByModuleDefinitionIdAndCode(md.getId(), objectCode));
    }

    public Optional<CustomEntityDefinition> findDefinitionById(Long id) {
        return definitionRepository.findById(id);
    }

    public Page<CustomEntityData> findAll(String tenantId, Long entityDefinitionId, Pageable pageable) {
        return dataRepository.findByTenantIdAndEntityDefinitionId(tenantId, entityDefinitionId, pageable);
    }

    public List<CustomEntityData> findAll(String tenantId, Long entityDefinitionId, int limit) {
        return dataRepository.findByTenantIdAndEntityDefinitionIdOrderByUpdatedAtDesc(
                tenantId, entityDefinitionId, Pageable.ofSize(limit));
    }

    public Optional<CustomEntityData> findById(Long id, String tenantId) {
        Optional<CustomEntityData> opt = dataRepository.findById(id);
        if (opt.isEmpty()) return Optional.empty();
        if (!tenantId.equals(opt.get().getTenantId())) return Optional.empty();
        return opt;
    }

    @Transactional
    public CustomEntityData create(String tenantId, Long entityDefinitionId, Map<String, Object> payloadMap) {
        CustomEntityData data = new CustomEntityData();
        data.setTenantId(tenantId);
        data.setEntityDefinition(definitionRepository.getReferenceById(entityDefinitionId));
        data.setPayload(toJson(payloadMap));
        return dataRepository.save(data);
    }

    @Transactional
    public CustomEntityData update(Long id, String tenantId, Map<String, Object> payloadMap) {
        CustomEntityData data = dataRepository.findById(id).orElseThrow();
        if (!tenantId.equals(data.getTenantId())) throw new SecurityException("Tenant mismatch");
        data.setPayload(toJson(payloadMap));
        return dataRepository.save(data);
    }

    @Transactional
    public void delete(Long id, String tenantId) {
        CustomEntityData data = dataRepository.findById(id).orElseThrow();
        if (!tenantId.equals(data.getTenantId())) throw new SecurityException("Tenant mismatch");
        dataRepository.delete(data);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map != null ? map : Map.of());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
