package fr.aplose.erp.modules.commerce.service;

import fr.aplose.erp.modules.catalog.repository.ProductRepository;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.entity.ProposalLine;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
import fr.aplose.erp.modules.commerce.web.dto.LineDto;
import fr.aplose.erp.modules.commerce.web.dto.ProposalDto;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
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
public class ProposalService {

    private final ProposalRepository repo;
    private final ThirdPartyRepository thirdPartyRepo;
    private final ContactRepository contactRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public Page<Proposal> findAll(String q, String status, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return repo.search(tid, q, pageable);
        if (status != null && !status.isBlank()) return repo.findByTenantIdAndStatus(tid, status, pageable);
        return repo.findByTenantId(tid, pageable);
    }

    @Transactional(readOnly = true)
    public Proposal findById(Long id) {
        return repo.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
    }

    @Transactional
    public Proposal create(ProposalDto dto, Long currentUserId) {
        String tid = TenantContext.getCurrentTenantId();

        Proposal p = new Proposal();
        p.setReference(generateReference(tid));
        p.setCreatedById(currentUserId);
        applyDto(p, dto, tid);
        return repo.save(p);
    }

    @Transactional
    public Proposal update(Long id, ProposalDto dto) {
        Proposal p = findById(id);
        if (!"DRAFT".equals(p.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }
        applyDto(p, dto, TenantContext.getCurrentTenantId());
        return repo.save(p);
    }

    @Transactional
    public ProposalLine addLine(Long proposalId, LineDto dto) {
        Proposal p = findById(proposalId);
        if (!"DRAFT".equals(p.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }
        String tid = TenantContext.getCurrentTenantId();

        ProposalLine line = new ProposalLine();
        line.setTenantId(tid);
        line.setProposal(p);
        line.setDescription(dto.getDescription());
        line.setQuantity(dto.getQuantity());
        line.setUnitPrice(dto.getUnitPrice());
        line.setDiscountPct(dto.getDiscountPct());
        line.setVatRate(dto.getVatRate());
        line.setCurrencyCode(p.getCurrencyCode());
        line.setSortOrder((short) p.getLines().size());

        if (dto.getProductId() != null) {
            productRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getProductId(), tid)
                    .ifPresent(line::setProduct);
        }

        line.recalculate();
        p.getLines().add(line);
        p.recalculate();
        repo.save(p);
        return line;
    }

    @Transactional
    public void removeLine(Long proposalId, Long lineId) {
        Proposal p = findById(proposalId);
        if (!"DRAFT".equals(p.getStatus())) {
            throw new IllegalStateException("Only draft proposals can be edited");
        }
        p.getLines().removeIf(l -> l.getId().equals(lineId));
        p.recalculate();
        repo.save(p);
    }

    @Transactional
    public void updateStatus(Long id, String newStatus) {
        Proposal p = findById(id);
        p.setStatus(newStatus);
        repo.save(p);
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return repo.countByTenantIdAndStatusIn(TenantContext.getCurrentTenantId(),
                Set.of("DRAFT", "SENT"));
    }

    private String generateReference(String tid) {
        int max = repo.findMaxReferenceNumber(tid);
        return String.format("PRO-%05d", max + 1);
    }

    private void applyDto(Proposal p, ProposalDto dto, String tid) {
        thirdPartyRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getThirdPartyId(), tid)
                .ifPresent(p::setThirdParty);
        if (dto.getContactId() != null) {
            contactRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getContactId(), tid)
                    .ifPresent(p::setContact);
        } else {
            p.setContact(null);
        }
        p.setTitle(dto.getTitle());
        p.setDateIssued(dto.getDateIssued());
        p.setDateValidUntil(dto.getDateValidUntil());
        p.setCurrencyCode(dto.getCurrencyCode());
        p.setDiscountAmount(dto.getDiscountAmount());
        p.setNotes(dto.getNotes());
        p.setTerms(dto.getTerms());
        if (dto.getSalesRepId() != null) {
            userRepo.findByIdAndTenantId(dto.getSalesRepId(), tid).ifPresent(p::setSalesRep);
        } else {
            p.setSalesRep(null);
        }
    }
}
