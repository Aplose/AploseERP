package fr.aplose.erp.modules.crm.service;

import fr.aplose.erp.modules.crm.entity.CrmActivity;
import fr.aplose.erp.modules.crm.repository.CrmActivityRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CrmActivityService {

    private final CrmActivityRepository repository;

    @Transactional(readOnly = true)
    public List<CrmActivity> findAll() {
        return repository.findByTenantIdOrderByDueDateAscCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<CrmActivity> findByThirdParty(Long thirdPartyId) {
        return repository.findByTenantIdAndThirdPartyIdOrderByDueDateAscCreatedAtDesc(
                TenantContext.getCurrentTenantId(), thirdPartyId);
    }

    @Transactional(readOnly = true)
    public List<CrmActivity> findPending() {
        return repository.findByTenantIdAndCompletedAtIsNullOrderByDueDateAsc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public Optional<CrmActivity> findById(Long id) {
        return repository.findById(id)
                .filter(a -> a.getTenantId().equals(TenantContext.getCurrentTenantId()));
    }

    @Transactional
    public CrmActivity save(CrmActivity activity) {
        return repository.save(activity);
    }

    @Transactional
    public void delete(Long id) {
        repository.findById(id)
                .filter(a -> a.getTenantId().equals(TenantContext.getCurrentTenantId()))
                .ifPresent(repository::delete);
    }

    @Transactional
    public void setCompleted(Long id, boolean completed) {
        findById(id).ifPresent(a -> {
            a.setCompletedAt(completed ? java.time.LocalDateTime.now() : null);
            repository.save(a);
        });
    }
}
