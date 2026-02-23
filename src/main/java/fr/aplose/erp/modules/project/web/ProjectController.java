package fr.aplose.erp.modules.project.web;

import fr.aplose.erp.dictionary.DictionaryType;
import fr.aplose.erp.dictionary.service.DictionaryService;
import fr.aplose.erp.modules.project.entity.Project;
import fr.aplose.erp.modules.project.service.ProjectService;
import fr.aplose.erp.modules.project.web.dto.ProjectDto;
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

@Controller
@RequestMapping("/projects")
@PreAuthorize("hasAuthority('PROJECT_READ')")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final DictionaryService dictionaryService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by("name"));
        model.addAttribute("projects", projectService.findAll(q, status, pageable));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "modules/project/project-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("project", projectService.findById(id));
        return "modules/project/project-detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("project", new ProjectDto());
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        return "modules/project/project-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('PROJECT_CREATE')")
    public String create(@Valid @ModelAttribute("project") ProjectDto dto,
                         BindingResult result,
                         @AuthenticationPrincipal ErpUserDetails principal,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/project/project-form";
        }
        try {
            Project p = projectService.create(dto, principal.getUserId());
            ra.addFlashAttribute("successMessage", "Project " + p.getCode() + " created");
            return "redirect:/projects/" + p.getId();
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/project/project-form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        Project p = projectService.findById(id);
        ProjectDto dto = new ProjectDto();
        dto.setCode(p.getCode());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setThirdPartyId(p.getThirdParty() != null ? p.getThirdParty().getId() : null);
        dto.setStatus(p.getStatus());
        dto.setPriority(p.getPriority());
        dto.setDateStart(p.getDateStart());
        dto.setDateEnd(p.getDateEnd());
        dto.setBudgetAmount(p.getBudgetAmount());
        dto.setCurrencyCode(p.getCurrencyCode());
        dto.setManagerId(p.getManager() != null ? p.getManager().getId() : null);
        dto.setBillingMode(p.getBillingMode());
        dto.setNotes(p.getNotes());
        model.addAttribute("project", dto);
        model.addAttribute("projectId", id);
        model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
        return "modules/project/project-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('PROJECT_UPDATE')")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("project") ProjectDto dto,
                        BindingResult result,
                        Model model,
                        RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("projectId", id);
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/project/project-form";
        }
        try {
            projectService.update(id, dto);
            ra.addFlashAttribute("successMessage", "Project updated");
            return "redirect:/projects/" + id;
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("projectId", id);
            model.addAttribute("currencies", dictionaryService.findByType(DictionaryType.CURRENCY));
            return "modules/project/project-form";
        }
    }
}
