package fr.aplose.erp.modules.commerce.web;

import fr.aplose.erp.modules.commerce.entity.PipelineStage;
import fr.aplose.erp.modules.commerce.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping
    @PreAuthorize("hasAuthority('PIPELINE_READ')")
    public String index(Model model) {
        List<PipelineStage> stages = pipelineService.findAllStages();
        model.addAttribute("stages", stages);
        model.addAttribute("forecast", pipelineService.getWeightedForecast());
        model.addAttribute("countByStage", pipelineService.getProposalCountByStage());
        model.addAttribute("pipelineService", pipelineService);
        return "modules/commerce/pipeline";
    }

    @PostMapping("/proposals/{id}/stage")
    @PreAuthorize("hasAuthority('PIPELINE_UPDATE')")
    public String moveProposal(@PathVariable Long id, @RequestParam(required = false) Long stageId, RedirectAttributes ra) {
        pipelineService.moveProposalToStage(id, stageId);
        ra.addFlashAttribute("message", "Proposal moved.");
        return "redirect:/pipeline";
    }

    @GetMapping("/stages")
    @PreAuthorize("hasAuthority('PIPELINE_UPDATE')")
    public String listStages(Model model) {
        model.addAttribute("stages", pipelineService.findAllStages());
        return "modules/commerce/pipeline-stages";
    }

    @GetMapping("/stages/new")
    @PreAuthorize("hasAuthority('PIPELINE_UPDATE')")
    public String newStageForm(Model model) {
        model.addAttribute("stage", new PipelineStage());
        return "modules/commerce/pipeline-stage-form";
    }

    @PostMapping("/stages")
    @PreAuthorize("hasAuthority('PIPELINE_UPDATE')")
    public String createStage(@ModelAttribute @Valid PipelineStage stage, BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return "modules/commerce/pipeline-stage-form";
        pipelineService.saveStage(stage);
        ra.addFlashAttribute("message", "Stage created.");
        return "redirect:/pipeline/stages";
    }

    @GetMapping("/stages/{id}/edit")
    @PreAuthorize("hasAuthority('PIPELINE_UPDATE')")
    public String editStageForm(@PathVariable Long id, Model model) {
        PipelineStage stage = pipelineService.getStageById(id);
        if (stage == null) return "redirect:/pipeline/stages";
        model.addAttribute("stage", stage);
        return "modules/commerce/pipeline-stage-form";
    }

    @PostMapping("/stages/{id}")
    @PreAuthorize("hasAuthority('PIPELINE_UPDATE')")
    public String updateStage(@PathVariable Long id, @ModelAttribute @Valid PipelineStage stage, BindingResult result, Model model, RedirectAttributes ra) {
        PipelineStage existing = pipelineService.getStageById(id);
        if (existing == null) return "redirect:/pipeline/stages";
        if (result.hasErrors()) {
            stage.setId(id);
            model.addAttribute("stage", stage);
            return "modules/commerce/pipeline-stage-form";
        }
        stage.setId(id);
        stage.setTenantId(existing.getTenantId());
        stage.setCreatedAt(existing.getCreatedAt());
        pipelineService.saveStage(stage);
        ra.addFlashAttribute("message", "Stage updated.");
        return "redirect:/pipeline/stages";
    }

    @PostMapping("/stages/{id}/delete")
    @PreAuthorize("hasAuthority('PIPELINE_UPDATE')")
    public String deleteStage(@PathVariable Long id, RedirectAttributes ra) {
        pipelineService.deleteStage(id);
        ra.addFlashAttribute("message", "Stage deleted.");
        return "redirect:/pipeline/stages";
    }
}
