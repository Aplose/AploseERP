package fr.aplose.erp.dolibarr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.aplose.erp.dolibarr.entity.DolibarrImportLog;
import fr.aplose.erp.dolibarr.entity.DolibarrImportRun;
import fr.aplose.erp.dolibarr.repository.DolibarrImportLogRepository;
import fr.aplose.erp.dolibarr.repository.DolibarrImportRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DolibarrImportLogService {

    private final DolibarrImportRunRepository runRepository;
    private final DolibarrImportLogRepository logRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DolibarrImportRun createRun(String tenantId, String dolibarrBaseUrl, Long configId, Long createdBy) {
        DolibarrImportRun run = new DolibarrImportRun();
        run.setTenantId(tenantId);
        run.setDolibarrBaseUrl(dolibarrBaseUrl);
        run.setConfigId(configId);
        run.setCreatedBy(createdBy);
        run.setStatus(DolibarrImportRun.STATUS_RUNNING);
        return runRepository.save(run);
    }

    @Transactional
    public void finishRun(Long runId, String status) {
        runRepository.findById(runId).ifPresent(run -> {
            run.setStatus(status);
            run.setFinishedAt(java.time.LocalDateTime.now());
            runRepository.save(run);
        });
    }

    @Transactional
    public void log(Long importRunId, String step, String level, String externalId, String entityType, Long entityId, String message, Object detail) {
        DolibarrImportLog logEntry = new DolibarrImportLog();
        logEntry.setImportRunId(importRunId);
        logEntry.setStep(step);
        logEntry.setLevel(level);
        logEntry.setExternalId(externalId);
        logEntry.setEntityType(entityType);
        logEntry.setEntityId(entityId);
        logEntry.setMessage(message != null ? (message.length() > 1000 ? message.substring(0, 1000) : message) : "");
        if (detail != null) {
            try {
                logEntry.setDetailJson(objectMapper.writeValueAsString(detail));
            } catch (JsonProcessingException ignored) {}
        }
        logRepository.save(logEntry);
    }

    public void logInfo(Long runId, String step, String message) {
        log(runId, step, DolibarrImportLog.LEVEL_INFO, null, null, null, message, null);
    }

    public void logWarn(Long runId, String step, String externalId, String message) {
        log(runId, step, DolibarrImportLog.LEVEL_WARN, externalId, null, null, message, null);
    }

    public void logError(Long runId, String step, String externalId, String message, Object detail) {
        log(runId, step, DolibarrImportLog.LEVEL_ERROR, externalId, null, null, message, detail);
    }

    public void logSkip(Long runId, String step, String externalId, String message) {
        log(runId, step, DolibarrImportLog.LEVEL_SKIP, externalId, null, null, message, null);
    }
}
