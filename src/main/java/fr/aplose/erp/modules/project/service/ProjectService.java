package fr.aplose.erp.modules.project.service;

import fr.aplose.erp.modules.project.entity.Project;
import fr.aplose.erp.modules.project.repository.ProjectRepository;
import fr.aplose.erp.modules.project.web.dto.ProjectDto;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final ThirdPartyRepository thirdPartyRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public Page<Project> findAll(String q, String status, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return projectRepo.search(tid, q, pageable);
        if (status != null && !status.isBlank()) return projectRepo.findByTenantIdAndStatus(tid, status, pageable);
        return projectRepo.findByTenantId(tid, pageable);
    }

    @Transactional(readOnly = true)
    public Project findById(Long id) {
        return projectRepo.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return projectRepo.countByTenantIdAndStatusIn(
                TenantContext.getCurrentTenantId(),
                Set.of("PLANNING", "ACTIVE")
        );
    }

    @Transactional
    public Project create(ProjectDto dto, Long currentUserId) {
        String tid = TenantContext.getCurrentTenantId();
        if (projectRepo.findByTenantIdAndCode(tid, dto.getCode()).isPresent()) {
            throw new IllegalStateException("Project code already exists: " + dto.getCode());
        }
        Project p = new Project();
        p.setCreatedById(currentUserId);
        applyDto(p, dto, tid);
        return projectRepo.save(p);
    }

    @Transactional
    public Project update(Long id, ProjectDto dto) {
        Project p = findById(id);
        String tid = TenantContext.getCurrentTenantId();
        projectRepo.findByTenantIdAndCode(tid, dto.getCode())
                .filter(proj -> !proj.getId().equals(id))
                .ifPresent(proj -> { throw new IllegalStateException("Project code already exists: " + dto.getCode()); });
        applyDto(p, dto, tid);
        return projectRepo.save(p);
    }

    private void applyDto(Project p, ProjectDto dto, String tid) {
        p.setCode(dto.getCode().trim().toUpperCase());
        p.setName(dto.getName().trim());
        p.setDescription(dto.getDescription());
        p.setStatus(dto.getStatus());
        p.setPriority(dto.getPriority());
        p.setDateStart(dto.getDateStart());
        p.setDateEnd(dto.getDateEnd());
        p.setBudgetAmount(dto.getBudgetAmount());
        p.setCurrencyCode(dto.getCurrencyCode());
        p.setBillingMode(dto.getBillingMode());
        p.setNotes(dto.getNotes());
        if (dto.getThirdPartyId() != null) {
            thirdPartyRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getThirdPartyId(), tid).ifPresent(p::setThirdParty);
        } else {
            p.setThirdParty(null);
        }
        if (dto.getManagerId() != null) {
            userRepo.findByIdAndTenantId(dto.getManagerId(), tid).ifPresent(p::setManager);
        } else {
            p.setManager(null);
        }
    }
}
