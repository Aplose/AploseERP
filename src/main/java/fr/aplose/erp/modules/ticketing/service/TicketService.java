package fr.aplose.erp.modules.ticketing.service;

import fr.aplose.erp.modules.ticketing.entity.Ticket;
import fr.aplose.erp.modules.ticketing.entity.TicketComment;
import fr.aplose.erp.modules.ticketing.repository.TicketCommentRepository;
import fr.aplose.erp.modules.ticketing.repository.TicketRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Ticket> findAll(String q, String status, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        if (q != null && !q.isBlank()) return ticketRepository.search(tid, q, pageable);
        if (status != null && !status.isBlank()) return ticketRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tid, status, pageable);
        return ticketRepository.findByTenantIdOrderByCreatedAtDesc(tid, pageable);
    }

    @Transactional(readOnly = true)
    public Ticket findById(Long id) {
        return ticketRepository.findByIdAndTenantIdWithComments(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }

    @Transactional
    public Ticket save(Ticket ticket, Long currentUserId) {
        if (ticket.getRequester() == null || ticket.getRequester().getId() == null) {
            ticket.setRequester(userRepository.findByIdAndTenantId(currentUserId, TenantContext.getCurrentTenantId()).orElse(null));
        }
        if (ticket.getAssignee() != null && ticket.getAssignee().getId() != null) {
            userRepository.findByIdAndTenantId(ticket.getAssignee().getId(), TenantContext.getCurrentTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not in tenant"));
        }
        return ticketRepository.save(ticket);
    }

    @Transactional
    public TicketComment addComment(Long ticketId, String content, Long currentUserId) {
        Ticket ticket = findById(ticketId);
        var user = userRepository.findByIdAndTenantId(currentUserId, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        TicketComment comment = new TicketComment();
        comment.setTenantId(TenantContext.getCurrentTenantId());
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setContent(content);
        return commentRepository.save(comment);
    }
}
