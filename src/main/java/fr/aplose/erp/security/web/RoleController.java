package fr.aplose.erp.security.web;

import fr.aplose.erp.security.service.RoleService;
import fr.aplose.erp.security.web.dto.RoleDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('ROLE_READ')")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return "modules/admin/roles/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String newForm(Model model) {
        model.addAttribute("role", new RoleDto());
        model.addAttribute("permissionsByModule", roleService.permissionsGroupedByModule());
        return "modules/admin/roles/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String create(@Valid @ModelAttribute("role") RoleDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("permissionsByModule", roleService.permissionsGroupedByModule());
            return "modules/admin/roles/form";
        }
        try {
            roleService.create(dto);
            ra.addFlashAttribute("successMessage", "Role created successfully");
            return "redirect:/admin/roles";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("permissionsByModule", roleService.permissionsGroupedByModule());
            return "modules/admin/roles/form";
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public String editForm(@PathVariable Long id, Model model) {
        var role = roleService.findById(id);
        var dto = new RoleDto();
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        role.getPermissions().forEach(p -> dto.getPermissionIds().add(p.getId()));

        model.addAttribute("role", dto);
        model.addAttribute("roleId", id);
        model.addAttribute("isSystem", role.isSystem());
        model.addAttribute("permissionsByModule", roleService.permissionsGroupedByModule());
        return "modules/admin/roles/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("role") RoleDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("roleId", id);
            model.addAttribute("permissionsByModule", roleService.permissionsGroupedByModule());
            return "modules/admin/roles/form";
        }
        try {
            roleService.update(id, dto);
            ra.addFlashAttribute("successMessage", "Role updated successfully");
            return "redirect:/admin/roles";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roleId", id);
            model.addAttribute("permissionsByModule", roleService.permissionsGroupedByModule());
            return "modules/admin/roles/form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            roleService.delete(id);
            ra.addFlashAttribute("successMessage", "Role deleted");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
