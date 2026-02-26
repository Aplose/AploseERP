package fr.aplose.erp.modules.commerce.service;

import fr.aplose.erp.modules.commerce.entity.BusinessContract;
import fr.aplose.erp.modules.commerce.repository.BusinessContractRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusinessContractService {

    private final BusinessContractRepository repository;

    @Transactional(readOnly = true)
    public List<BusinessContract> findAll() {
        return repository.findByTenantIdOrderByStartDateDesc(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public List<BusinessContract> findByThirdParty(Long thirdPartyId) {
        return repository.findByTenantIdAndThirdPartyIdOrderByStartDateDesc(
                TenantContext.getCurrentTenantId(), thirdPartyId);
    }

    @Transactional(readOnly = true)
    public Optional<BusinessContract> findById(Long id) {
        return repository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId());
    }

    @Transactional
    public BusinessContract save(BusinessContract contract) {
        return repository.save(contract);
    }

    @Transactional
    public void delete(Long id) {
        repository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId()).ifPresent(repository::delete);
    }
}
