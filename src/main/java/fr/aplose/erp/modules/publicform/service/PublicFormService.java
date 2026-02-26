package fr.aplose.erp.modules.publicform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.aplose.erp.mail.service.MailService;
import fr.aplose.erp.modules.publicform.dto.FormFieldDto;
import fr.aplose.erp.modules.publicform.entity.PublicForm;
import fr.aplose.erp.modules.publicform.entity.PublicFormSubmission;
import fr.aplose.erp.modules.publicform.repository.PublicFormRepository;
import fr.aplose.erp.modules.publicform.repository.PublicFormSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicFormService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PublicFormRepository formRepository;
    private final PublicFormSubmissionRepository submissionRepository;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public List<PublicForm> findByTenant(String tenantId) {
        return formRepository.findByTenantIdOrderByNameAsc(tenantId);
    }

    @Transactional(readOnly = true)
    public PublicForm findByIdAndTenant(Long id, String tenantId) {
        return formRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Form not found: " + id));
    }

    @Transactional(readOnly = true)
    public PublicForm findByTenantAndCodeForPublic(String tenantId, String code) {
        return formRepository.findByTenantIdAndCodeAndEnabledTrue(tenantId, code)
                .orElseThrow(() -> new IllegalArgumentException("Form not found or disabled: " + code));
    }

    @Transactional
    public PublicForm save(PublicForm form, String tenantId) {
        form.setTenantId(tenantId);
        if (form.getId() == null) {
            if (formRepository.existsByTenantIdAndCode(tenantId, form.getCode())) {
                throw new IllegalArgumentException("publicform.code.exists");
            }
        } else {
            if (formRepository.existsByTenantIdAndCodeAndIdNot(tenantId, form.getCode(), form.getId())) {
                throw new IllegalArgumentException("publicform.code.exists");
            }
        }
        return formRepository.save(form);
    }

    @Transactional
    public void deleteById(Long id, String tenantId) {
        PublicForm form = findByIdAndTenant(id, tenantId);
        formRepository.delete(form);
    }

    /**
     * Parse form fields JSON for display in template.
     */
    public List<FormFieldDto> getFormFields(PublicForm form) {
        if (form.getFieldsJson() == null || form.getFieldsJson().isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> raw = OBJECT_MAPPER.readValue(form.getFieldsJson(), new TypeReference<>() {});
            return raw.stream().map(FormFieldDto::fromMap).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to parse form fields JSON: {}", e.getMessage());
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Page<PublicFormSubmission> getSubmissionsByForm(Long formId, String tenantId, Pageable pageable) {
        PublicForm form = findByIdAndTenant(formId, tenantId);
        return submissionRepository.findByFormIdOrderBySubmittedAtDesc(form.getId(), pageable);
    }

    @Transactional
    public PublicFormSubmission submit(String tenantId, String formCode, Map<String, String> data, String ipAddress) {
        PublicForm form = findByTenantAndCodeForPublic(tenantId, formCode);
        String dataJson;
        try {
            dataJson = OBJECT_MAPPER.writeValueAsString(data != null ? data : Map.of());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid submission data");
        }
        PublicFormSubmission sub = new PublicFormSubmission();
        sub.setForm(form);
        sub.setTenantId(tenantId);
        sub.setDataJson(dataJson);
        sub.setIpAddress(ipAddress);
        sub.setSubmittedAt(LocalDateTime.now());
        sub = submissionRepository.save(sub);

        notifyIfConfigured(form, sub, data);
        return sub;
    }

    private void notifyIfConfigured(PublicForm form, PublicFormSubmission sub, Map<String, String> data) {
        String emails = form.getNotifyEmails();
        if (emails == null || emails.isBlank()) return;
        String subject = "Formulaire \"" + form.getName() + "\" – nouvelle soumission";
        StringBuilder body = new StringBuilder("Nouvelle soumission reçue le ").append(sub.getSubmittedAt()).append("\n\n");
        if (data != null) {
            for (Map.Entry<String, String> e : data.entrySet()) {
                if ("captchaAnswer".equals(e.getKey())) continue;
                body.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
        }
        for (String to : emails.split("[,;]")) {
            String t = to.trim();
            if (!t.isEmpty()) {
                mailService.sendSimple(t, subject, body.toString());
            }
        }
        sub.setNotifiedAt(LocalDateTime.now());
        submissionRepository.save(sub);
    }
}
