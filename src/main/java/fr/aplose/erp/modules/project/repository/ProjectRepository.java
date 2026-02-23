package fr.aplose.erp.modules.project.repository;

import fr.aplose.erp.modules.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByTenantId(String tenantId, Pageable pageable);

    Page<Project> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);

    Optional<Project> findByIdAndTenantId(Long id, String tenantId);

    Optional<Project> findByTenantIdAndCode(String tenantId, String code);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tid " +
           "AND (LOWER(p.code) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Project> search(@Param("tid") String tenantId, @Param("q") String q, Pageable pageable);

    long countByTenantIdAndStatusIn(String tenantId, java.util.Set<String> statuses);
}
