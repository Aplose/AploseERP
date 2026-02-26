package fr.aplose.erp.modules.commerce.repository;

import fr.aplose.erp.modules.commerce.entity.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {

    List<PipelineStage> findByTenantIdOrderBySortOrderAsc(String tenantId);
}
