package fr.aplose.erp.modules.leave.web;

import fr.aplose.erp.modules.leave.service.LeaveRequestService;
import fr.aplose.erp.modules.leave.service.LeaveTypeService;
import fr.aplose.erp.modules.leave.web.dto.LeaveRequestDenyDto;
import fr.aplose.erp.modules.leave.web.dto.LeaveRequestFormDto;
import fr.aplose.erp.security.service.ErpUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/leave-requests")
@PreAuthorize("hasAuthority('LEAVE_READ')")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveTypeService leaveTypeService;
    private final MessageSource messageSource;

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) Long requesterId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());
        var requests = leaveRequestService.findByFilters(status, requesterId, fromDate, toDate, pageable);
        model.addAttribute("requests", requests);
        model.addAttribute("leaveTypes", leaveTypeService.findAllForCurrentTenant());
        model.addAttribute("status", status);
        model.addAttribute("requesterId", requesterId);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        return "modules/leave/leave-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var request = leaveRequestService.findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        model.addAttribute("request", request);
        return "modules/leave/leave-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("request", new LeaveRequestFormDto());
        model.addAttribute("leaveTypes", leaveTypeService.findAllForCurrentTenant());
        return "modules/leave/leave-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('LEAVE_CREATE')")
    public String create(@Valid @ModelAttribute("request") LeaveRequestFormDto dto,
                         BindingResult result,
                         @AuthenticationPrincipal ErpUserDetails principal,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("leaveTypes", leaveTypeService.findAllForCurrentTenant());
            return "modules/leave/leave-form";
        }
        try {
            var lr = leaveRequestService.create(
                    principal.getUserId(),
                    dto.getLeaveTypeId(),
                    dto.getDateStart(),
                    dto.getDateEnd(),
                    dto.isHalfDayStart(),
                    dto.isHalfDayEnd(),
                    dto.getComment()
            );
            ra.addFlashAttribute("successMessage", messageSource.getMessage("leave.created", null, LocaleContextHolder.getLocale()));
            return "redirect:/leave-requests/" + lr.getId();
        } catch (IllegalArgumentException e) {
            result.reject("error", e.getMessage());
            model.addAttribute("leaveTypes", leaveTypeService.findAllForCurrentTenant());
            return "modules/leave/leave-form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('LEAVE_UPDATE')")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails principal, Model model) {
        var lr = leaveRequestService.findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"DRAFT".equals(lr.getStatus()) && !"DENIED".equals(lr.getStatus())) {
            return "redirect:/leave-requests/" + id;
        }
        if (!lr.getRequester().getId().equals(principal.getUserId())) {
            return "redirect:/leave-requests/" + id;
        }
        var dto = new LeaveRequestFormDto();
        dto.setLeaveTypeId(lr.getLeaveType().getId());
        dto.setDateStart(lr.getDateStart());
        dto.setDateEnd(lr.getDateEnd());
        dto.setHalfDayStart(lr.isHalfDayStart());
        dto.setHalfDayEnd(lr.isHalfDayEnd());
        dto.setComment(lr.getComment());
        model.addAttribute("request", dto);
        model.addAttribute("requestId", id);
        model.addAttribute("leaveTypes", leaveTypeService.findAllForCurrentTenant());
        return "modules/leave/leave-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('LEAVE_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("request") LeaveRequestFormDto dto,
                         BindingResult result,
                         @AuthenticationPrincipal ErpUserDetails principal,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("requestId", id);
            model.addAttribute("leaveTypes", leaveTypeService.findAllForCurrentTenant());
            return "modules/leave/leave-form";
        }
        try {
            leaveRequestService.update(id, principal.getUserId(), dto.getLeaveTypeId(), dto.getDateStart(), dto.getDateEnd(),
                    dto.isHalfDayStart(), dto.isHalfDayEnd(), dto.getComment());
            ra.addFlashAttribute("successMessage", messageSource.getMessage("leave.updated", null, LocaleContextHolder.getLocale()));
            return "redirect:/leave-requests/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("requestId", id);
            model.addAttribute("leaveTypes", leaveTypeService.findAllForCurrentTenant());
            return "modules/leave/leave-form";
        }
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('LEAVE_UPDATE')")
    public String submit(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails principal, RedirectAttributes ra) {
        try {
            leaveRequestService.submit(id, principal.getUserId());
            ra.addFlashAttribute("successMessage", messageSource.getMessage("leave.submitted", null, LocaleContextHolder.getLocale()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave-requests/" + id;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails principal, RedirectAttributes ra) {
        try {
            leaveRequestService.approve(id, principal.getUserId());
            ra.addFlashAttribute("successMessage", messageSource.getMessage("leave.approved", null, LocaleContextHolder.getLocale()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave-requests/" + id;
    }

    @GetMapping("/{id}/deny")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    public String denyForm(@PathVariable Long id, Model model) {
        var lr = leaveRequestService.findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"PENDING_APPROVAL".equals(lr.getStatus())) {
            return "redirect:/leave-requests/" + id;
        }
        model.addAttribute("request", lr);
        model.addAttribute("denyDto", new LeaveRequestDenyDto());
        return "modules/leave/leave-deny";
    }

    @PostMapping("/{id}/deny")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE')")
    public String deny(@PathVariable Long id,
                       @ModelAttribute("denyDto") LeaveRequestDenyDto dto,
                       @AuthenticationPrincipal ErpUserDetails principal,
                       RedirectAttributes ra) {
        try {
            leaveRequestService.deny(id, principal.getUserId(), dto.getResponseComment());
            ra.addFlashAttribute("successMessage", messageSource.getMessage("leave.denied", null, LocaleContextHolder.getLocale()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/leave-requests/" + id;
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('LEAVE_UPDATE')")
    public String reopen(@PathVariable Long id, @AuthenticationPrincipal ErpUserDetails principal, RedirectAttributes ra) {
        try {
            leaveRequestService.reopen(id, principal.getUserId());
            ra.addFlashAttribute("successMessage", messageSource.getMessage("leave.reopened", null, LocaleContextHolder.getLocale()));
            return "redirect:/leave-requests/" + id + "/edit";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/leave-requests/" + id;
        }
    }
}
