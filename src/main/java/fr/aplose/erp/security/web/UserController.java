package fr.aplose.erp.security.web;

import fr.aplose.erp.security.service.RoleService;
import fr.aplose.erp.security.service.UserService;
import fr.aplose.erp.security.web.dto.ChangePasswordDto;
import fr.aplose.erp.security.web.dto.UserCreateDto;
import fr.aplose.erp.security.web.dto.UserEditDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAuthority('USER_READ')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        var pageable = PageRequest.of(page, size, Sort.by("lastName", "firstName"));
        var users = q.isBlank() ? userService.findAll(pageable) : userService.search(q, pageable);
        model.addAttribute("users", users);
        model.addAttribute("q", q);
        return "modules/admin/users/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("user", new UserCreateDto());
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("locales", availableLocales());
        return "modules/admin/users/form-create";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String create(@Valid @ModelAttribute("user") UserCreateDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }
        if (result.hasErrors()) {
            model.addAttribute("roles", roleService.findAll());
            model.addAttribute("locales", availableLocales());
            return "modules/admin/users/form-create";
        }
        try {
            userService.create(dto);
            ra.addFlashAttribute("successMessage", "User created successfully");
            return "redirect:/admin/users";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", roleService.findAll());
            model.addAttribute("locales", availableLocales());
            return "modules/admin/users/form-create";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var user = userService.findById(id);
        var dto = new UserEditDto();
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setLocale(user.getLocale());
        dto.setTimezone(user.getTimezone());
        dto.setActive(user.isActive());
        dto.setTenantAdmin(user.isTenantAdmin());
        user.getRoles().forEach(r -> dto.getRoleIds().add(r.getId()));

        model.addAttribute("user", dto);
        model.addAttribute("userId", id);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("roles", roleService.findAll());
        model.addAttribute("locales", availableLocales());
        return "modules/admin/users/form-edit";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("user") UserEditDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("roles", roleService.findAll());
            model.addAttribute("locales", availableLocales());
            return "modules/admin/users/form-edit";
        }
        try {
            userService.update(id, dto);
            ra.addFlashAttribute("successMessage", "User updated successfully");
            return "redirect:/admin/users";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("roles", roleService.findAll());
            model.addAttribute("locales", availableLocales());
            return "modules/admin/users/form-edit";
        }
    }

    @GetMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public String passwordForm(@PathVariable Long id, Model model) {
        model.addAttribute("dto", new ChangePasswordDto());
        model.addAttribute("userId", id);
        model.addAttribute("username", userService.findById(id).getUsername());
        return "modules/admin/users/form-password";
    }

    @PostMapping("/{id}/password")
    @PreAuthorize("hasAuthority('USER_ADMIN')")
    public String changePassword(@PathVariable Long id,
                                 @Valid @ModelAttribute("dto") ChangePasswordDto dto,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes ra) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }
        if (result.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("username", userService.findById(id).getUsername());
            return "modules/admin/users/form-password";
        }
        userService.changePassword(id, dto);
        ra.addFlashAttribute("successMessage", "Password changed successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public String toggleActive(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleActive(id);
        ra.addFlashAttribute("successMessage", "User status updated");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        userService.softDelete(id);
        ra.addFlashAttribute("successMessage", "User deleted");
        return "redirect:/admin/users";
    }

    private java.util.List<java.util.Locale> availableLocales() {
        return java.util.List.of(
            java.util.Locale.ENGLISH,
            java.util.Locale.FRENCH
        );
    }
}
