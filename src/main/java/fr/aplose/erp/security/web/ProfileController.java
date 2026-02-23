package fr.aplose.erp.security.web;

import fr.aplose.erp.security.service.ErpUserDetails;
import fr.aplose.erp.security.service.UserService;
import fr.aplose.erp.security.web.dto.ProfilePasswordDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String view(@AuthenticationPrincipal ErpUserDetails principal, Model model) {
        var user = userService.findById(principal.getUserId());
        model.addAttribute("user", user);
        return "auth/profile";
    }

    @GetMapping("/password")
    public String passwordForm(Model model) {
        model.addAttribute("dto", new ProfilePasswordDto());
        return "auth/profile-password";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal ErpUserDetails principal,
                                 @Valid ProfilePasswordDto dto,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes ra) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }
        if (result.hasErrors()) {
            return "auth/profile-password";
        }
        try {
            userService.changeOwnPassword(principal.getUserId(), dto.getCurrentPassword(), dto.getNewPassword());
            ra.addFlashAttribute("successMessage", "Password changed successfully");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/profile-password";
        }
    }
}
