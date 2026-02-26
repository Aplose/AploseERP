package fr.aplose.erp.modules.hr.repository;

import fr.aplose.erp.modules.hr.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {

    List<JobPosition> findByTenantIdOrderByCode(String tenantId);
}
