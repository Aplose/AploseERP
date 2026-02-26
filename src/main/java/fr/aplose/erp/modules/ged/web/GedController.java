package fr.aplose.erp.modules.ged.web;

import fr.aplose.erp.modules.ged.service.GedService;
import fr.aplose.erp.security.service.ErpUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/ged")
@PreAuthorize("hasAuthority('GED_READ')")
@RequiredArgsConstructor
public class GedController {

    private final GedService gedService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("documents", gedService.search(q, pageable));
        model.addAttribute("q", q);
        return "modules/ged/document-list";
    }

    @GetMapping("/upload")
    @PreAuthorize("hasAuthority('GED_CREATE')")
    public String uploadForm(Model model) {
        model.addAttribute("entityTypes", List.of("THIRD_PARTY", "CONTACT", "INVOICE", "PROPOSAL", "PROJECT", "OTHER"));
        return "modules/ged/upload-form";
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('GED_CREATE')")
    public String upload(@RequestParam String entityType,
                         @RequestParam Long entityId,
                         @RequestParam("file") MultipartFile file,
                         @AuthenticationPrincipal ErpUserDetails user,
                         RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file.");
            return "redirect:/ged/upload";
        }
        try {
            gedService.upload(entityType, entityId, file, user.getUserId());
            redirectAttributes.addFlashAttribute("message", "Document uploaded.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/ged";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('GED_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        gedService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Document deleted.");
        return "redirect:/ged";
    }
}
