package fr.aplose.erp.modules.project.repository;

import fr.aplose.erp.modules.project.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {

    List<ProjectTask> findByProjectIdAndParentTaskIsNullOrderBySortOrderAsc(Long projectId);
}
