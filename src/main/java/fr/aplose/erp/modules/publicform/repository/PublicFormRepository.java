package fr.aplose.erp.modules.publicform.repository;

import fr.aplose.erp.modules.publicform.entity.PublicForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicFormRepository extends JpaRepository<PublicForm, Long> {

    List<PublicForm> findByTenantIdOrderByNameAsc(String tenantId);

    Optional<PublicForm> findByTenantIdAndCodeAndEnabledTrue(String tenantId, String code);

    Optional<PublicForm> findByIdAndTenantId(Long id, String tenantId);

    boolean existsByTenantIdAndCode(String tenantId, String code);

    boolean existsByTenantIdAndCodeAndIdNot(String tenantId, String code, Long id);
}
