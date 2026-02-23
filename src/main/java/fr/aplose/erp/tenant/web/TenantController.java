package fr.aplose.erp.tenant.web;

import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.entity.Tenant;
import fr.aplose.erp.tenant.repository.TenantRepository;
import fr.aplose.erp.tenant.web.dto.TenantDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_READ')")
    public String form(Model model) {
        Tenant tenant = getCurrentTenant();
        TenantDto dto = toDto(tenant);
        model.addAttribute("tenant", dto);
        model.addAttribute("tenantCode", tenant.getCode());
        model.addAttribute("tenantId", tenant.getId());
        return "modules/admin/settings";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_UPDATE')")
    public String save(@Valid TenantDto dto,
                       BindingResult result,
                       Model model,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            Tenant tenant = getCurrentTenant();
            model.addAttribute("tenantCode", tenant.getCode());
            model.addAttribute("tenantId", tenant.getId());
            return "modules/admin/settings";
        }
        Tenant tenant = getCurrentTenant();
        fromDto(tenant, dto);
        tenantRepository.save(tenant);
        ra.addFlashAttribute("successMessage", "Settings saved successfully");
        return "redirect:/admin/settings";
    }

    private Tenant getCurrentTenant() {
        String tenantId = TenantContext.getCurrentTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
    }

    private static TenantDto toDto(Tenant t) {
        TenantDto dto = new TenantDto();
        dto.setName(t.getName());
        dto.setLegalName(t.getLegalName());
        dto.setAddressLine1(t.getAddressLine1());
        dto.setAddressLine2(t.getAddressLine2());
        dto.setCity(t.getCity());
        dto.setStateProvince(t.getStateProvince());
        dto.setPostalCode(t.getPostalCode());
        dto.setCountryCode(t.getCountryCode());
        dto.setPhone(t.getPhone());
        dto.setEmail(t.getEmail());
        dto.setWebsite(t.getWebsite());
        dto.setDefaultLocale(t.getDefaultLocale());
        dto.setDefaultCurrency(t.getDefaultCurrency());
        dto.setTimezone(t.getTimezone());
        dto.setFiscalYearStart(t.getFiscalYearStart());
        return dto;
    }

    private static void fromDto(Tenant t, TenantDto dto) {
        t.setName(dto.getName());
        t.setLegalName(dto.getLegalName());
        t.setAddressLine1(dto.getAddressLine1());
        t.setAddressLine2(dto.getAddressLine2());
        t.setCity(dto.getCity());
        t.setStateProvince(dto.getStateProvince());
        t.setPostalCode(dto.getPostalCode());
        t.setCountryCode(dto.getCountryCode());
        t.setPhone(dto.getPhone());
        t.setEmail(dto.getEmail());
        t.setWebsite(dto.getWebsite());
        t.setDefaultLocale(dto.getDefaultLocale());
        t.setDefaultCurrency(dto.getDefaultCurrency());
        t.setTimezone(dto.getTimezone());
        t.setFiscalYearStart(dto.getFiscalYearStart());
    }
}
