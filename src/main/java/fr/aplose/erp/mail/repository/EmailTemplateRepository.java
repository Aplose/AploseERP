package fr.aplose.erp.mail.repository;

import fr.aplose.erp.mail.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Optional<EmailTemplate> findByTemplateKeyAndLocale(String templateKey, String locale);

    List<EmailTemplate> findAllByOrderByTemplateKeyAscLocaleAsc();
}
