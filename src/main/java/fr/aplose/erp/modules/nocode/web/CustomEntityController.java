package fr.aplose.erp.modules.nocode.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.aplose.erp.modules.nocode.entity.CustomEntityData;
import fr.aplose.erp.modules.nocode.entity.CustomEntityDefinition;
import fr.aplose.erp.modules.nocode.service.CustomEntityService;
import fr.aplose.erp.tenant.context.TenantContext;
import fr.aplose.erp.tenant.service.TenantModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/app/{moduleCode}/{objectCode}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CUSTOM_ENTITY_READ')")
public class CustomEntityController {

    private final CustomEntityService customEntityService;
    private final TenantModuleService tenantModuleService;
    private static final ObjectMapper JSON = new ObjectMapper();

    @GetMapping
    public String list(@PathVariable String moduleCode,
                       @PathVariable String objectCode,
                       Model model,
                       RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || !tenantModuleService.isModuleEnabled(tenantId, "module:" + moduleCode)) {
            ra.addFlashAttribute("errorMessage", "module.disabled.redirect");
            return "redirect:/dashboard?moduleDisabled=1";
        }
        Optional<CustomEntityDefinition> defOpt = customEntityService.findDefinitionByModuleAndObjectCode(moduleCode, objectCode);
        if (defOpt.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Not found");
            return "redirect:/dashboard";
        }
        CustomEntityDefinition def = defOpt.get();
        List<CustomEntityData> items = customEntityService.findAll(tenantId, def.getId(), 500);
        List<String> columns = parseListColumns(def.getListColumns());
        List<Map<String, Object>> payloads = items.stream()
                .map(d -> customEntityService.parsePayload(d.getPayload()))
                .toList();
        model.addAttribute("definition", def);
        model.addAttribute("items", items);
        model.addAttribute("columns", columns);
        model.addAttribute("payloads", payloads);
        model.addAttribute("moduleCode", moduleCode);
        model.addAttribute("objectCode", objectCode);
        return "modules/nocode/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('CUSTOM_ENTITY_CREATE')")
    public String newForm(@PathVariable String moduleCode,
                         @PathVariable String objectCode,
                         Model model,
                         RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || !tenantModuleService.isModuleEnabled(tenantId, "module:" + moduleCode)) {
            return "redirect:/dashboard?moduleDisabled=1";
        }
        Optional<CustomEntityDefinition> defOpt = customEntityService.findDefinitionByModuleAndObjectCode(moduleCode, objectCode);
        if (defOpt.isEmpty()) return "redirect:/dashboard";
        model.addAttribute("definition", defOpt.get());
        model.addAttribute("fieldSpecs", parseFieldsSchema(defOpt.get().getFieldsSchema()));
        model.addAttribute("moduleCode", moduleCode);
        model.addAttribute("objectCode", objectCode);
        model.addAttribute("payload", Map.<String, Object>of());
        model.addAttribute("formAction", "/app/" + moduleCode + "/" + objectCode);
        return "modules/nocode/form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOM_ENTITY_CREATE')")
    public String create(@PathVariable String moduleCode,
                         @PathVariable String objectCode,
                         @RequestParam Map<String, String> params,
                         RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || !tenantModuleService.isModuleEnabled(tenantId, "module:" + moduleCode)) {
            return "redirect:/dashboard?moduleDisabled=1";
        }
        Optional<CustomEntityDefinition> defOpt = customEntityService.findDefinitionByModuleAndObjectCode(moduleCode, objectCode);
        if (defOpt.isEmpty()) return "redirect:/dashboard";
        Map<String, Object> payload = parseParamsToPayload(params);
        customEntityService.create(tenantId, defOpt.get().getId(), payload);
        ra.addFlashAttribute("successMessage", "Enregistrement créé.");
        return "redirect:/app/" + moduleCode + "/" + objectCode;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String moduleCode,
                         @PathVariable String objectCode,
                         @PathVariable Long id,
                         Model model,
                         RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || !tenantModuleService.isModuleEnabled(tenantId, "module:" + moduleCode)) {
            return "redirect:/dashboard?moduleDisabled=1";
        }
        Optional<CustomEntityDefinition> defOpt = customEntityService.findDefinitionByModuleAndObjectCode(moduleCode, objectCode);
        if (defOpt.isEmpty()) return "redirect:/dashboard";
        Optional<CustomEntityData> dataOpt = customEntityService.findById(id, tenantId);
        if (dataOpt.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Not found");
            return "redirect:/app/" + moduleCode + "/" + objectCode;
        }
        model.addAttribute("definition", defOpt.get());
        model.addAttribute("data", dataOpt.get());
        model.addAttribute("payload", customEntityService.parsePayload(dataOpt.get().getPayload()));
        model.addAttribute("moduleCode", moduleCode);
        model.addAttribute("objectCode", objectCode);
        return "modules/nocode/detail";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('CUSTOM_ENTITY_UPDATE')")
    public String editForm(@PathVariable String moduleCode,
                          @PathVariable String objectCode,
                          @PathVariable Long id,
                          Model model,
                          RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || !tenantModuleService.isModuleEnabled(tenantId, "module:" + moduleCode)) {
            return "redirect:/dashboard?moduleDisabled=1";
        }
        Optional<CustomEntityDefinition> defOpt = customEntityService.findDefinitionByModuleAndObjectCode(moduleCode, objectCode);
        if (defOpt.isEmpty()) return "redirect:/dashboard";
        Optional<CustomEntityData> dataOpt = customEntityService.findById(id, tenantId);
        if (dataOpt.isEmpty()) return "redirect:/app/" + moduleCode + "/" + objectCode;
        model.addAttribute("definition", defOpt.get());
        model.addAttribute("data", dataOpt.get());
        Map<String, Object> payload = customEntityService.parsePayload(dataOpt.get().getPayload());
        model.addAttribute("payload", payload);
        model.addAttribute("fieldSpecs", parseFieldsSchema(defOpt.get().getFieldsSchema()));
        model.addAttribute("moduleCode", moduleCode);
        model.addAttribute("objectCode", objectCode);
        model.addAttribute("formAction", "/app/" + moduleCode + "/" + objectCode + "/" + id);
        return "modules/nocode/form";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOM_ENTITY_UPDATE')")
    public String update(@PathVariable String moduleCode,
                         @PathVariable String objectCode,
                         @PathVariable Long id,
                         @RequestParam Map<String, String> params,
                         RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || !tenantModuleService.isModuleEnabled(tenantId, "module:" + moduleCode)) {
            return "redirect:/dashboard?moduleDisabled=1";
        }
        Map<String, Object> payload = parseParamsToPayload(params);
        customEntityService.update(id, tenantId, payload);
        ra.addFlashAttribute("successMessage", "Enregistrement mis à jour.");
        return "redirect:/app/" + moduleCode + "/" + objectCode + "/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('CUSTOM_ENTITY_DELETE')")
    public String delete(@PathVariable String moduleCode,
                         @PathVariable String objectCode,
                         @PathVariable Long id,
                         RedirectAttributes ra) {
        String tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null || !tenantModuleService.isModuleEnabled(tenantId, "module:" + moduleCode)) {
            return "redirect:/dashboard?moduleDisabled=1";
        }
        customEntityService.delete(id, tenantId);
        ra.addFlashAttribute("successMessage", "Enregistrement supprimé.");
        return "redirect:/app/" + moduleCode + "/" + objectCode;
    }

    private Map<String, Object> parseParamsToPayload(Map<String, String> params) {
        Map<String, Object> payload = new java.util.HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if ("_csrf".equals(e.getKey())) continue;
            payload.put(e.getKey(), e.getValue());
        }
        return payload;
    }

    private static List<String> parseListColumns(String listColumnsJson) {
        if (listColumnsJson == null || listColumnsJson.isBlank()) return List.of();
        try {
            return JSON.readValue(listColumnsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<Map<String, Object>> parseFieldsSchema(String fieldsSchemaJson) {
        if (fieldsSchemaJson == null || fieldsSchemaJson.isBlank()) return List.of();
        try {
            return JSON.readValue(fieldsSchemaJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
