package fr.aplose.erp.modules.thirdparty.service;

import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.modules.thirdparty.web.dto.ThirdPartyDto;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ThirdPartyService {

    private final ThirdPartyRepository repo;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ThirdParty> findAll(String q, String filter, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return repo.search(tid, q, pageable);
        if ("customers".equals(filter))  return repo.findByTenantIdAndCustomerTrueAndDeletedAtIsNull(tid, pageable);
        if ("suppliers".equals(filter))  return repo.findByTenantIdAndSupplierTrueAndDeletedAtIsNull(tid, pageable);
        return repo.findByTenantIdAndDeletedAtIsNull(tid, pageable);
    }

    @Transactional(readOnly = true)
    public ThirdParty findById(Long id) {
        return repo.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Third party not found: " + id));
    }

    @Transactional
    public ThirdParty create(ThirdPartyDto dto, Long currentUserId) {
        String tid = TenantContext.getCurrentTenantId();
        repo.findByCodeAndTenantIdAndDeletedAtIsNull(dto.getCode(), tid)
            .ifPresent(t -> { throw new IllegalStateException("Code already in use: " + dto.getCode()); });

        ThirdParty tp = new ThirdParty();
        tp.setCreatedById(currentUserId);
        applyDto(tp, dto, tid);
        return repo.save(tp);
    }

    @Transactional
    public ThirdParty update(Long id, ThirdPartyDto dto) {
        ThirdParty tp = findById(id);
        String tid = TenantContext.getCurrentTenantId();
        repo.findByCodeAndTenantIdAndDeletedAtIsNull(dto.getCode(), tid)
            .filter(t -> !t.getId().equals(id))
            .ifPresent(t -> { throw new IllegalStateException("Code already in use: " + dto.getCode()); });
        applyDto(tp, dto, tid);
        return repo.save(tp);
    }

    @Transactional
    public void delete(Long id) {
        ThirdParty tp = findById(id);
        tp.setDeletedAt(LocalDateTime.now());
        tp.setStatus("INACTIVE");
        repo.save(tp);
    }

    private void applyDto(ThirdParty tp, ThirdPartyDto dto, String tid) {
        tp.setCode(dto.getCode().trim().toUpperCase());
        tp.setName(dto.getName());
        tp.setCustomer(dto.isCustomer());
        tp.setSupplier(dto.isSupplier());
        tp.setProspect(dto.isProspect());
        // derive type
        if (dto.isCustomer() && dto.isSupplier()) tp.setType("BOTH");
        else if (dto.isCustomer())  tp.setType("CUSTOMER");
        else if (dto.isSupplier())  tp.setType("SUPPLIER");
        else if (dto.isProspect())  tp.setType("PROSPECT");
        else tp.setType("OTHER");
        tp.setLegalForm(dto.getLegalForm());
        tp.setTaxId(dto.getTaxId());
        tp.setRegistrationNo(dto.getRegistrationNo());
        tp.setWebsite(dto.getWebsite());
        tp.setPhone(dto.getPhone());
        tp.setFax(dto.getFax());
        tp.setEmail(dto.getEmail());
        tp.setAddressLine1(dto.getAddressLine1());
        tp.setAddressLine2(dto.getAddressLine2());
        tp.setCity(dto.getCity());
        tp.setStateProvince(dto.getStateProvince());
        tp.setPostalCode(dto.getPostalCode());
        tp.setCountryCode(dto.getCountryCode());
        tp.setCurrencyCode(dto.getCurrencyCode());
        tp.setPaymentTerms(dto.getPaymentTerms());
        tp.setCreditLimit(dto.getCreditLimit());
        tp.setTags(dto.getTags());
        tp.setNotes(dto.getNotes());
        tp.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        if (dto.getSalesRepId() != null) {
            userRepository.findByIdAndTenantId(dto.getSalesRepId(), tid)
                          .ifPresent(tp::setSalesRep);
        } else {
            tp.setSalesRep(null);
        }
    }
}
