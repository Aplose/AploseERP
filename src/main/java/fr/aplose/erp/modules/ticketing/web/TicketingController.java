package fr.aplose.erp.modules.ticketing.web;

import fr.aplose.erp.modules.ticketing.entity.Ticket;
import fr.aplose.erp.modules.ticketing.service.TicketService;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ticketing")
@PreAuthorize("hasAuthority('TICKETING_READ')")
@RequiredArgsConstructor
public class TicketingController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                      @RequestParam(defaultValue = "") String status,
                      @RequestParam(defaultValue = "0") int page,
                      Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("tickets", ticketService.findAll(q, status.isBlank() ? null : status, pageable));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "modules/ticketing/ticket-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('TICKETING_CREATE')")
    public String newForm(Model model) {
        String tid = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
                ? fr.aplose.erp.tenant.context.TenantContext.getCurrentTenantId() : null;
        model.addAttribute("ticket", new Ticket());
        model.addAttribute("users", tid != null ? userRepository.findByTenantIdAndDeletedAtIsNull(tid, org.springframework.data.domain.PageRequest.of(0, 500, org.springframework.data.domain.Sort.by("lastName"))).getContent() : java.util.List.of());
        return "modules/ticketing/ticket-form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TICKETING_CREATE')")
    public String create(@ModelAttribute Ticket ticket,
                        @RequestParam(required = false) Long assigneeId,
                        @AuthenticationPrincipal ErpUserDetails user,
                        RedirectAttributes redirectAttributes) {
        if (assigneeId != null) {
            String tid = fr.aplose.erp.tenant.context.TenantContext.getCurrentTenantId();
            ticket.setAssignee(userRepository.findByIdAndTenantId(assigneeId, tid).orElse(null));
        }
        ticketService.save(ticket, user.getUserId());
        redirectAttributes.addFlashAttribute("message", "Ticket created.");
        return "redirect:/ticketing";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("ticket", ticketService.findById(id));
        return "modules/ticketing/ticket-detail";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('TICKETING_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        String tid = fr.aplose.erp.tenant.context.TenantContext.getCurrentTenantId();
        model.addAttribute("ticket", ticketService.findById(id));
        model.addAttribute("users", userRepository.findByTenantIdAndDeletedAtIsNull(tid, org.springframework.data.domain.PageRequest.of(0, 500, org.springframework.data.domain.Sort.by("lastName"))).getContent());
        return "modules/ticketing/ticket-form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('TICKETING_UPDATE')")
    public String update(@PathVariable Long id, @ModelAttribute Ticket ticket,
                        @RequestParam(required = false) Long assigneeId,
                        RedirectAttributes redirectAttributes) {
        Ticket existing = ticketService.findById(id);
        existing.setSubject(ticket.getSubject());
        existing.setDescription(ticket.getDescription());
        existing.setStatus(ticket.getStatus());
        existing.setPriority(ticket.getPriority());
        if (assigneeId != null) {
            String tid = fr.aplose.erp.tenant.context.TenantContext.getCurrentTenantId();
            existing.setAssignee(userRepository.findByIdAndTenantId(assigneeId, tid).orElse(null));
        } else {
            existing.setAssignee(null);
        }
        ticketService.save(existing, existing.getRequester() != null ? existing.getRequester().getId() : null);
        redirectAttributes.addFlashAttribute("message", "Ticket updated.");
        return "redirect:/ticketing/" + id;
    }

    @PostMapping("/{id}/comment")
    @PreAuthorize("hasAuthority('TICKETING_UPDATE')")
    public String addComment(@PathVariable Long id, @RequestParam String content,
                            @AuthenticationPrincipal ErpUserDetails user,
                            RedirectAttributes redirectAttributes) {
        if (content != null && !content.isBlank()) {
            ticketService.addComment(id, content.trim(), user.getUserId());
            redirectAttributes.addFlashAttribute("message", "Comment added.");
        }
        return "redirect:/ticketing/" + id;
    }
}
