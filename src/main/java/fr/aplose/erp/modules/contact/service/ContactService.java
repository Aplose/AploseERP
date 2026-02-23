package fr.aplose.erp.modules.contact.service;

import fr.aplose.erp.modules.contact.entity.Contact;
import fr.aplose.erp.modules.contact.entity.ContactThirdPartyLink;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.contact.repository.ContactThirdPartyLinkRepository;
import fr.aplose.erp.modules.contact.web.dto.ContactDto;
import fr.aplose.erp.modules.contact.web.dto.ContactThirdPartyLinkDto;
import fr.aplose.erp.modules.thirdparty.entity.ThirdParty;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository repo;
    private final ContactThirdPartyLinkRepository linkRepository;
    private final ThirdPartyRepository thirdPartyRepository;

    @Transactional(readOnly = true)
    public Page<Contact> findAll(String q, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return repo.search(tid, q, pageable);
        return repo.findByTenantIdAndDeletedAtIsNull(tid, pageable);
    }

    @Transactional(readOnly = true)
    public List<Contact> findByThirdParty(Long thirdPartyId) {
        return linkRepository.findByThirdPartyId(thirdPartyId).stream()
                .map(ContactThirdPartyLink::getContact)
                .filter(c -> c.getDeletedAt() == null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Contact findById(Long id) {
        return repo.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + id));
    }

    @Transactional
    public Contact create(ContactDto dto) {
        Contact c = new Contact();
        applyDto(c, dto);
        return repo.save(c);
    }

    @Transactional
    public Contact update(Long id, ContactDto dto) {
        Contact c = findById(id);
        applyDto(c, dto);
        return repo.save(c);
    }

    @Transactional
    public void delete(Long id) {
        Contact c = findById(id);
        c.setDeletedAt(LocalDateTime.now());
        c.setStatus("INACTIVE");
        repo.save(c);
    }

    private void applyDto(Contact c, ContactDto dto) {
        c.setCivility(dto.getCivility());
        c.setFirstName(dto.getFirstName());
        c.setLastName(dto.getLastName());
        c.setJobTitle(dto.getJobTitle());
        c.setDepartment(dto.getDepartment());
        c.setEmail(dto.getEmail());
        c.setEmailSecondary(dto.getEmailSecondary());
        c.setPhone(dto.getPhone());
        c.setPhoneMobile(dto.getPhoneMobile());
        c.setFax(dto.getFax());
        c.setAddressLine1(dto.getAddressLine1());
        c.setAddressLine2(dto.getAddressLine2());
        c.setCity(dto.getCity());
        c.setStateProvince(dto.getStateProvince());
        c.setPostalCode(dto.getPostalCode());
        c.setCountryCode(dto.getCountryCode());
        c.setNotes(dto.getNotes());
        c.setPrimary(dto.isPrimary());
        c.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        String tid = TenantContext.getCurrentTenantId();
        c.getThirdPartyLinks().clear();
        if (dto.getLinks() != null) {
            for (ContactThirdPartyLinkDto linkDto : dto.getLinks()) {
                if (linkDto.getThirdPartyId() == null) continue;
                thirdPartyRepository.findByIdAndTenantIdAndDeletedAtIsNull(linkDto.getThirdPartyId(), tid)
                        .ifPresent(tp -> {
                            ContactThirdPartyLink link = new ContactThirdPartyLink();
                            link.setTenantId(tid);
                            link.setContact(c);
                            link.setThirdParty(tp);
                            link.setLinkTypeCode(linkDto.getLinkTypeCode() != null ? linkDto.getLinkTypeCode() : "SALARIE");
                            c.getThirdPartyLinks().add(link);
                        });
            }
        }
    }
}
