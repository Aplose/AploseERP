package fr.aplose.erp.modules.agenda.web;

import fr.aplose.erp.modules.agenda.entity.AgendaEvent;
import fr.aplose.erp.modules.agenda.service.AgendaEventService;
import fr.aplose.erp.modules.agenda.web.dto.AgendaEventDto;
import fr.aplose.erp.security.service.ErpUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/agenda")
@PreAuthorize("hasAuthority('AGENDA_READ')")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaEventService agendaEventService;

    @GetMapping
    public String list(@RequestParam(required = false) LocalDate from,
                       @RequestParam(required = false) LocalDate to,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 50, Sort.by("startDatetime"));
        model.addAttribute("events", agendaEventService.findAll(from, to, pageable));
        model.addAttribute("from", from != null ? from : LocalDate.now());
        model.addAttribute("to", to != null ? to : LocalDate.now().plusMonths(1));
        return "modules/agenda/agenda-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("event", agendaEventService.findById(id));
        return "modules/agenda/agenda-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('AGENDA_CREATE')")
    public String newForm(Model model) {
        AgendaEventDto dto = new AgendaEventDto();
        dto.setStartDatetime(LocalDateTime.now().withMinute(0).withSecond(0).withNano(0));
        dto.setEndDatetime(dto.getStartDatetime().plusHours(1));
        model.addAttribute("event", dto);
        return "modules/agenda/agenda-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('AGENDA_CREATE')")
    public String create(@Valid @ModelAttribute("event") AgendaEventDto dto,
                          BindingResult result,
                          @AuthenticationPrincipal ErpUserDetails principal,
                          Model model,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "modules/agenda/agenda-form";
        }
        AgendaEvent e = agendaEventService.create(dto, principal.getUserId());
        ra.addFlashAttribute("successMessage", "Event created");
        return "redirect:/agenda/" + e.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('AGENDA_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        AgendaEvent e = agendaEventService.findById(id);
        AgendaEventDto dto = new AgendaEventDto();
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setType(e.getType());
        dto.setStatus(e.getStatus());
        dto.setAllDay(e.isAllDay());
        dto.setStartDatetime(e.getStartDatetime());
        dto.setEndDatetime(e.getEndDatetime());
        dto.setLocation(e.getLocation());
        dto.setThirdPartyId(e.getThirdParty() != null ? e.getThirdParty().getId() : null);
        dto.setContactId(e.getContact() != null ? e.getContact().getId() : null);
        dto.setProjectId(e.getProject() != null ? e.getProject().getId() : null);
        dto.setColor(e.getColor());
        dto.setPrivacy(e.isPrivacy());
        model.addAttribute("event", dto);
        model.addAttribute("eventId", id);
        return "modules/agenda/agenda-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('AGENDA_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("event") AgendaEventDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("eventId", id);
            return "modules/agenda/agenda-form";
        }
        agendaEventService.update(id, dto);
        ra.addFlashAttribute("successMessage", "Event updated");
        return "redirect:/agenda/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('AGENDA_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        agendaEventService.delete(id);
        ra.addFlashAttribute("successMessage", "Event deleted");
        return "redirect:/agenda";
    }
}
