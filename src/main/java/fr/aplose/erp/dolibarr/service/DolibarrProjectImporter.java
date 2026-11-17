package fr.aplose.erp.dolibarr.service;

import fr.aplose.erp.dolibarr.client.DolibarrApiClient;
import fr.aplose.erp.modules.project.entity.Project;
import fr.aplose.erp.modules.project.entity.ProjectTask;
import fr.aplose.erp.modules.project.repository.ProjectRepository;
import fr.aplose.erp.modules.project.repository.ProjectTaskRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports Dolibarr projects and their tasks into the AploseERP project module.
 */
@Service
@RequiredArgsConstructor
public class DolibarrProjectImporter {

    private static final String STEP_PROJECTS = "PROJECTS";
    private static final String STEP_TASKS = "TASKS";

    private final DolibarrApiClient client;
    private final DolibarrImportLogService logService;
    private final DolibarrMappingService mappingService;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ThirdPartyRepository thirdPartyRepository;

    public void importProjects(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        List<Map<String, Object>> list = client.getAll("projects", Map.of("sortfield", "t.rowid"));
        int alreadyImported = 0;
        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;
            if (mappingService.isImported(tenantId, "projects", doliId)) {
                alreadyImported++;
                continue;
            }
            try {
                String code = DolibarrImportHelper.getString(m, "ref");
                if (code == null || code.isBlank()) code = "PJ-" + doliId;
                var existing = projectRepository.findByTenantIdAndCode(tenantId, code);
                if (existing.isPresent()) {
                    mappingService.record(tenantId, runId, "projects", doliId, "PROJECT", existing.get().getId());
                    logService.logInfo(runId, STEP_PROJECTS, "Dolibarr id " + doliId + " rattaché à l'existant (code " + code + ")");
                    continue;
                }

                Project p = new Project();
                p.setCode(code);
                String name = DolibarrImportHelper.getString(m, "title");
                p.setName(name != null && !name.isBlank() ? name : code);
                p.setDescription(DolibarrImportHelper.getString(m, "description"));
                Long socId = DolibarrImportHelper.getLong(m, "socid");
                if (socId != null) {
                    mappingService.find(tenantId, "thirdparties", socId)
                            .flatMap(thirdPartyRepository::findById)
                            .filter(tp -> tenantId.equals(tp.getTenantId()))
                            .ifPresent(p::setThirdParty);
                }
                p.setStatus(mapProjectStatus(DolibarrImportHelper.getStatus(m)));
                p.setDateStart(firstDate(m, "date_start", "dateo"));
                p.setDateEnd(firstDate(m, "date_end", "datee"));
                p.setBudgetAmount(DolibarrImportHelper.getBigDecimal(m, "budget_amount"));
                p.setNotes(DolibarrImportHelper.getString(m, "note_private"));

                Project saved = projectRepository.save(p);
                mappingService.record(tenantId, runId, "projects", doliId, "PROJECT", saved.getId());
            } catch (Exception e) {
                logService.logError(runId, STEP_PROJECTS, String.valueOf(doliId), e.getMessage(), null);
            }
        }
        logService.logInfo(runId, STEP_PROJECTS, alreadyImported + " projet(s) déjà importé(s) lors d'un import précédent");
    }

    /**
     * Tasks are created first, then linked to their parent task in a second pass,
     * since a sub-task may come before its parent in the Dolibarr list.
     */
    public void importTasks(Long runId) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return;

        List<Map<String, Object>> list = client.getAll("tasks", Map.of("sortfield", "t.rowid"));
        Map<Long, Long> parentByDoliTask = new HashMap<>();
        int alreadyImported = 0;
        for (Map<String, Object> m : list) {
            Long doliId = DolibarrImportHelper.getDolibarrId(m);
            if (doliId == null) continue;
            if (mappingService.isImported(tenantId, "tasks", doliId)) {
                alreadyImported++;
                continue;
            }
            try {
                Long doliProjectId = DolibarrImportHelper.getLong(m, "fk_project");
                if (doliProjectId == null) doliProjectId = DolibarrImportHelper.getLong(m, "fk_projet");
                Project project = mappingService.find(tenantId, "projects", doliProjectId)
                        .flatMap(id -> projectRepository.findByIdAndTenantId(id, tenantId))
                        .orElse(null);
                if (project == null) {
                    logService.logSkip(runId, STEP_TASKS, String.valueOf(doliId), "Project not found: " + doliProjectId);
                    continue;
                }

                ProjectTask t = new ProjectTask();
                t.setTenantId(tenantId);
                t.setProject(project);
                String title = DolibarrImportHelper.getString(m, "label");
                if (title == null || title.isBlank()) title = DolibarrImportHelper.getString(m, "ref");
                t.setTitle(title != null && !title.isBlank() ? title : "Tâche " + doliId);
                t.setDescription(DolibarrImportHelper.getString(m, "description"));
                t.setDateStart(firstDate(m, "date_start", "dateo"));
                t.setDateDue(firstDate(m, "date_end", "datee"));
                Integer progress = DolibarrImportHelper.getInteger(m, "progress");
                int pct = progress != null ? Math.max(0, Math.min(100, progress)) : 0;
                t.setProgressPct((short) pct);
                t.setStatus(pct >= 100 ? "DONE" : pct > 0 ? "IN_PROGRESS" : "TODO");
                t.setEstimatedHours(secondsToHours(DolibarrImportHelper.getBigDecimal(m, "planned_workload")));
                BigDecimal logged = secondsToHours(DolibarrImportHelper.getBigDecimal(m, "duration_effective"));
                t.setLoggedHours(logged != null ? logged : BigDecimal.ZERO);
                Integer rank = DolibarrImportHelper.getInteger(m, "rang");
                if (rank != null) t.setSortOrder((short) Math.max(0, Math.min(Short.MAX_VALUE, rank)));

                ProjectTask saved = taskRepository.save(t);
                mappingService.record(tenantId, runId, "tasks", doliId, "PROJECT_TASK", saved.getId());

                Long parent = DolibarrImportHelper.getLong(m, "fk_task_parent");
                if (parent != null && parent > 0) parentByDoliTask.put(doliId, parent);
            } catch (Exception e) {
                logService.logError(runId, STEP_TASKS, String.valueOf(doliId), e.getMessage(), null);
            }
        }

        for (Map.Entry<Long, Long> e : parentByDoliTask.entrySet()) {
            var child = mappingService.find(tenantId, "tasks", e.getKey()).flatMap(taskRepository::findById);
            var parent = mappingService.find(tenantId, "tasks", e.getValue()).flatMap(taskRepository::findById);
            if (child.isPresent() && parent.isPresent()) {
                child.get().setParentTask(parent.get());
                taskRepository.save(child.get());
            } else {
                logService.logWarn(runId, STEP_TASKS, String.valueOf(e.getKey()), "Parent task not found: " + e.getValue());
            }
        }
        logService.logInfo(runId, STEP_TASKS, alreadyImported + " tâche(s) déjà importée(s) lors d'un import précédent");
    }

    /** Dolibarr project status: 0 draft, 1 open, 2 closed. */
    static String mapProjectStatus(Integer status) {
        if (status == null) return "PLANNING";
        return switch (status) {
            case 1 -> "ACTIVE";
            case 2 -> "COMPLETED";
            default -> "PLANNING";
        };
    }

    private static java.time.LocalDate firstDate(Map<String, Object> m, String key, String fallbackKey) {
        java.time.LocalDate d = DolibarrImportHelper.getLocalDate(m, key);
        return d != null ? d : DolibarrImportHelper.getLocalDate(m, fallbackKey);
    }

    /** Dolibarr stores workloads and durations in seconds. */
    private static BigDecimal secondsToHours(BigDecimal seconds) {
        if (seconds == null) return null;
        return seconds.divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }
}
