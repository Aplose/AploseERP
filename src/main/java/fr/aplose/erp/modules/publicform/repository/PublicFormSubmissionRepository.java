package fr.aplose.erp.modules.publicform.repository;

import fr.aplose.erp.modules.publicform.entity.PublicFormSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicFormSubmissionRepository extends JpaRepository<PublicFormSubmission, Long> {

    Page<PublicFormSubmission> findByFormIdOrderBySubmittedAtDesc(Long formId, Pageable pageable);

    Page<PublicFormSubmission> findByTenantIdOrderBySubmittedAtDesc(String tenantId, Pageable pageable);
}
