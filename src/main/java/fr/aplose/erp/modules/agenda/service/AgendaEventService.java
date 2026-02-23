package fr.aplose.erp.modules.agenda.service;

import fr.aplose.erp.modules.agenda.entity.AgendaEvent;
import fr.aplose.erp.modules.agenda.repository.AgendaEventRepository;
import fr.aplose.erp.modules.agenda.web.dto.AgendaEventDto;
import fr.aplose.erp.modules.contact.repository.ContactRepository;
import fr.aplose.erp.modules.project.repository.ProjectRepository;
import fr.aplose.erp.modules.thirdparty.repository.ThirdPartyRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaEventService {

    private final AgendaEventRepository eventRepo;
    private final ThirdPartyRepository thirdPartyRepo;
    private final ContactRepository contactRepo;
    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<AgendaEvent> findUpcoming(int days) {
        String tid = TenantContext.getCurrentTenantId();
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = LocalDate.now().plusDays(days).atTime(LocalTime.MAX);
        return eventRepo.findBetween(tid, from, to);
    }

    @Transactional(readOnly = true)
    public Page<AgendaEvent> findAll(LocalDate from, LocalDate to, Pageable pageable) {
        String tid = TenantContext.getCurrentTenantId();
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : LocalDateTime.now();
        LocalDateTime toDt = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.now().plusYears(1);
        return eventRepo.findByTenantIdAndStartDatetimeBetweenOrderByStartDatetimeAsc(tid, fromDt, toDt, pageable);
    }

    @Transactional(readOnly = true)
    public AgendaEvent findById(Long id) {
        return eventRepo.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
    }

    @Transactional
    public AgendaEvent create(AgendaEventDto dto, Long organizerUserId) {
        String tid = TenantContext.getCurrentTenantId();
        AgendaEvent e = new AgendaEvent();
        e.setTenantId(tid);
        e.setOrganizer(userRepo.findByIdAndTenantId(organizerUserId, tid)
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
        applyDto(e, dto, tid);
        return eventRepo.save(e);
    }

    @Transactional
    public AgendaEvent update(Long id, AgendaEventDto dto) {
        AgendaEvent e = findById(id);
        applyDto(e, dto, TenantContext.getCurrentTenantId());
        return eventRepo.save(e);
    }

    @Transactional
    public void delete(Long id) {
        AgendaEvent e = findById(id);
        eventRepo.delete(e);
    }

    private void applyDto(AgendaEvent e, AgendaEventDto dto, String tid) {
        e.setTitle(dto.getTitle().trim());
        e.setDescription(dto.getDescription());
        e.setType(dto.getType());
        e.setStatus(dto.getStatus());
        e.setAllDay(dto.isAllDay());
        e.setStartDatetime(dto.getStartDatetime());
        e.setEndDatetime(dto.getEndDatetime());
        e.setLocation(dto.getLocation());
        e.setColor(dto.getColor());
        e.setPrivacy(dto.isPrivacy());
        if (dto.getThirdPartyId() != null) {
            thirdPartyRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getThirdPartyId(), tid).ifPresent(e::setThirdParty);
        } else {
            e.setThirdParty(null);
        }
        if (dto.getContactId() != null) {
            contactRepo.findByIdAndTenantIdAndDeletedAtIsNull(dto.getContactId(), tid).ifPresent(e::setContact);
        } else {
            e.setContact(null);
        }
        if (dto.getProjectId() != null) {
            projectRepo.findByIdAndTenantId(dto.getProjectId(), tid).ifPresent(e::setProject);
        } else {
            e.setProject(null);
        }
    }
}
