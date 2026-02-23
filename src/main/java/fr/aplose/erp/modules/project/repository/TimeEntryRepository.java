package fr.aplose.erp.modules.project.repository;

import fr.aplose.erp.modules.project.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByProjectIdOrderByDateWorkedDesc(Long projectId, org.springframework.data.domain.Pageable pageable);
}
