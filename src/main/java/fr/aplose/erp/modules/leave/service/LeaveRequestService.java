package fr.aplose.erp.modules.leave.service;

import fr.aplose.erp.mail.service.MailService;
import fr.aplose.erp.modules.leave.entity.LeaveRequest;
import fr.aplose.erp.modules.leave.entity.LeaveType;
import fr.aplose.erp.modules.leave.repository.LeaveRequestRepository;
import fr.aplose.erp.security.entity.User;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeService leaveTypeService;
    private final UserRepository userRepository;
    private final MailService mailService;

    public Optional<LeaveRequest> findById(Long id) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return Optional.empty();
        return leaveRequestRepository.findByIdAndTenantId(id, tenantId);
    }

    public Page<LeaveRequest> findByFilters(String status, Long requesterId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) return Page.empty(pageable);
        return leaveRequestRepository.findByTenantIdAndFilters(tenantId, status, requesterId, fromDate, toDate, pageable);
    }

    @Transactional
    public LeaveRequest create(Long requesterId, Long leaveTypeId, LocalDate dateStart, LocalDate dateEnd,
                               boolean halfDayStart, boolean halfDayEnd, String comment) {
        User requester = userRepository.findByIdAndTenantId(requesterId, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        LeaveType leaveType = leaveTypeService.findByIdAndTenant(leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
        if (dateEnd.isBefore(dateStart)) {
            throw new IllegalArgumentException("dateEnd must be >= dateStart");
        }
        LeaveRequest lr = new LeaveRequest();
        lr.setRequester(requester);
        lr.setLeaveType(leaveType);
        lr.setDateStart(dateStart);
        lr.setDateEnd(dateEnd);
        lr.setHalfDayStart(halfDayStart);
        lr.setHalfDayEnd(halfDayEnd);
        lr.setComment(comment);
        lr.setStatus("DRAFT");
        return leaveRequestRepository.save(lr);
    }

    @Transactional
    public LeaveRequest update(Long id, Long userId, Long leaveTypeId, LocalDate dateStart, LocalDate dateEnd,
                               boolean halfDayStart, boolean halfDayEnd, String comment) {
        LeaveRequest lr = findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"DRAFT".equals(lr.getStatus())) {
            throw new IllegalStateException("Only draft requests can be edited");
        }
        if (!lr.getRequester().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the requester can edit this request");
        }
        LeaveType leaveType = leaveTypeService.findByIdAndTenant(leaveTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
        if (dateEnd.isBefore(dateStart)) {
            throw new IllegalArgumentException("dateEnd must be >= dateStart");
        }
        lr.setLeaveType(leaveType);
        lr.setDateStart(dateStart);
        lr.setDateEnd(dateEnd);
        lr.setHalfDayStart(halfDayStart);
        lr.setHalfDayEnd(halfDayEnd);
        lr.setComment(comment);
        return leaveRequestRepository.save(lr);
    }

    @Transactional
    public void submit(Long id, Long userId) {
        LeaveRequest lr = findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"DRAFT".equals(lr.getStatus())) {
            throw new IllegalStateException("Only draft requests can be submitted");
        }
        if (!lr.getRequester().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the requester can submit this request");
        }
        User validator = lr.getRequester().getLeaveValidator();
        if (validator == null) {
            throw new IllegalStateException("No leave validator defined for this user. Please set a validator on the user profile.");
        }
        lr.setStatus("PENDING_APPROVAL");
        lr.setSubmittedAt(LocalDateTime.now());
        lr.setValidator(validator);
        leaveRequestRepository.save(lr);

        String requestUrl = mailService.getBaseUrl() + "/leave-requests/" + lr.getId();
        try {
            mailService.sendLeaveRequestSubmittedToValidator(
                    validator.getEmail(),
                    validator.getDisplayName(),
                    lr.getRequester().getDisplayName(),
                    lr.getLeaveType().getLabel(),
                    lr.getDateStart().format(DATE_FMT),
                    lr.getDateEnd().format(DATE_FMT),
                    requestUrl
            );
        } catch (Exception e) {
            // Log but do not fail the workflow
        }
    }

    @Transactional
    public void approve(Long id, Long userId) {
        LeaveRequest lr = findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"PENDING_APPROVAL".equals(lr.getStatus())) {
            throw new IllegalStateException("Only pending requests can be approved");
        }
        User validator = lr.getValidator();
        if (validator == null || !validator.getId().equals(userId)) {
            throw new IllegalArgumentException("Only the designated validator can approve this request");
        }
        lr.setStatus("APPROVED");
        lr.setApprovedById(userId);
        lr.setApprovedAt(LocalDateTime.now());
        leaveRequestRepository.save(lr);
    }

    @Transactional
    public void deny(Long id, Long userId, String responseComment) {
        LeaveRequest lr = findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"PENDING_APPROVAL".equals(lr.getStatus())) {
            throw new IllegalStateException("Only pending requests can be denied");
        }
        User validator = lr.getValidator();
        if (validator == null || !validator.getId().equals(userId)) {
            throw new IllegalArgumentException("Only the designated validator can deny this request");
        }
        lr.setStatus("DENIED");
        lr.setDeniedById(userId);
        lr.setDeniedAt(LocalDateTime.now());
        lr.setResponseComment(responseComment != null ? responseComment : "");
        leaveRequestRepository.save(lr);

        String requestUrl = mailService.getBaseUrl() + "/leave-requests/" + lr.getId();
        try {
            mailService.sendLeaveRequestDeniedToRequester(
                    lr.getRequester().getEmail(),
                    lr.getRequester().getDisplayName(),
                    lr.getResponseComment(),
                    requestUrl
            );
        } catch (Exception e) {
            // Log but do not fail the workflow
        }
    }

    @Transactional
    public void reopen(Long id, Long userId) {
        LeaveRequest lr = findById(id).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!"DENIED".equals(lr.getStatus())) {
            throw new IllegalStateException("Only denied requests can be reopened");
        }
        if (!lr.getRequester().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the requester can reopen this request");
        }
        lr.setStatus("DRAFT");
        leaveRequestRepository.save(lr);
    }
}
