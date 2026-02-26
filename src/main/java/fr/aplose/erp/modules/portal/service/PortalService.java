package fr.aplose.erp.modules.portal.service;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
import fr.aplose.erp.modules.ged.entity.GedDocument;
import fr.aplose.erp.modules.ged.repository.GedDocumentRepository;
import fr.aplose.erp.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortalService {

    private static final String ENTITY_TYPE_THIRD_PARTY = "THIRD_PARTY";

    private final UserRepository userRepository;
    private final ProposalRepository proposalRepository;
    private final InvoiceRepository invoiceRepository;
    private final GedDocumentRepository gedDocumentRepository;

    @Transactional(readOnly = true)
    public Long getThirdPartyIdForUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getThirdParty() != null)
                .map(u -> u.getThirdParty().getId())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Proposal> getProposalsForThirdParty(String tenantId, Long thirdPartyId) {
        return proposalRepository.findByTenantIdAndThirdPartyIdOrderByDateIssuedDesc(tenantId, thirdPartyId);
    }

    @Transactional(readOnly = true)
    public Optional<Proposal> getProposalIfBelongsToThirdParty(Long id, String tenantId, Long thirdPartyId) {
        return proposalRepository.findByIdAndTenantId(id, tenantId)
                .filter(p -> p.getThirdParty() != null && p.getThirdParty().getId().equals(thirdPartyId));
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesForThirdParty(String tenantId, Long thirdPartyId) {
        return invoiceRepository.findByTenantIdAndThirdPartyIdOrderByDateIssuedDesc(tenantId, thirdPartyId);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceIfBelongsToThirdParty(Long id, String tenantId, Long thirdPartyId) {
        return invoiceRepository.findByIdAndTenantId(id, tenantId)
                .filter(inv -> inv.getThirdParty() != null && inv.getThirdParty().getId().equals(thirdPartyId));
    }

    @Transactional(readOnly = true)
    public List<GedDocument> getDocumentsForThirdParty(String tenantId, Long thirdPartyId) {
        return gedDocumentRepository.findByTenantIdAndEntityTypeAndEntityIdOrderByVersionDesc(
                tenantId, ENTITY_TYPE_THIRD_PARTY, thirdPartyId);
    }

    @Transactional(readOnly = true)
    public Optional<GedDocument> getDocumentIfBelongsToThirdParty(Long id, String tenantId, Long thirdPartyId) {
        return gedDocumentRepository.findByIdAndTenantId(id, tenantId)
                .filter(d -> ENTITY_TYPE_THIRD_PARTY.equals(d.getEntityType()) && thirdPartyId.equals(d.getEntityId()));
    }
}
