package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.entity.DolibarrImportMapping;
import fr.aplose.erp.dolibarr.repository.DolibarrImportMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Correspondence between Dolibarr records and AploseERP records, kept across import runs.
 * <p>
 * Looking mappings up across runs is what makes the import re-runnable: a record imported by a
 * previous run is not created again, and documents imported later still find their third party.
 */
@Service
@RequiredArgsConstructor
public class DolibarrMappingService {

    private final DolibarrImportMappingRepository mappingRepository;

    /** AploseERP id of an already imported Dolibarr record, from any run. */
    public Optional<Long> find(String tenantId, String dolibarrEntity, Long dolibarrId) {
        if (dolibarrId == null) return Optional.empty();
        return mappingRepository
                .findFirstByTenantIdAndDolibarrEntityAndDolibarrIdOrderByIdDesc(tenantId, dolibarrEntity, dolibarrId)
                .map(DolibarrImportMapping::getAploseId);
    }

    public boolean isImported(String tenantId, String dolibarrEntity, Long dolibarrId) {
        return find(tenantId, dolibarrEntity, dolibarrId).isPresent();
    }

    public void record(String tenantId, Long runId, String dolibarrEntity, Long dolibarrId,
                       String aploseEntity, Long aploseId) {
        DolibarrImportMapping mapping = new DolibarrImportMapping();
        mapping.setTenantId(tenantId);
        mapping.setImportRunId(runId);
        mapping.setDolibarrEntity(dolibarrEntity);
        mapping.setDolibarrId(dolibarrId);
        mapping.setAploseEntity(aploseEntity);
        mapping.setAploseId(aploseId);
        mappingRepository.save(mapping);
    }
}
