package fr.aplose.erp.modules.commerce.service;

import fr.aplose.erp.modules.commerce.entity.Invoice;
import fr.aplose.erp.modules.commerce.entity.Proposal;
import fr.aplose.erp.modules.commerce.repository.InvoiceRepository;
import fr.aplose.erp.modules.commerce.repository.ProposalRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final InvoiceRepository invoiceRepository;
    private final ProposalRepository proposalRepository;

    private static final int PROPOSAL_FOLLOW_UP_DAYS = 7;

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdue(TenantContext.getCurrentTenantId(), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Proposal> getProposalsToFollowUp() {
        LocalDate today = LocalDate.now();
        LocalDate limitDate = today.minusDays(PROPOSAL_FOLLOW_UP_DAYS);
        return proposalRepository.findProposalsToFollowUp(TenantContext.getCurrentTenantId(), today, limitDate);
    }
}
