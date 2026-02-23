package fr.aplose.erp.mail.service;

import fr.aplose.erp.mail.entity.EmailTemplate;
import fr.aplose.erp.mail.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;

    public Optional<EmailTemplate> findByKeyAndLocale(String key, String locale) {
        return emailTemplateRepository.findByTemplateKeyAndLocale(key, locale != null ? locale : "fr");
    }

    public String render(String content, Map<String, String> variables) {
        if (content == null) return "";
        String result = content;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplate> findAll() {
        return emailTemplateRepository.findAllByOrderByTemplateKeyAscLocaleAsc();
    }

    @Transactional
    public EmailTemplate save(EmailTemplate template) {
        return emailTemplateRepository.save(template);
    }

    public Optional<EmailTemplate> findById(Long id) {
        return emailTemplateRepository.findById(id);
    }
}
