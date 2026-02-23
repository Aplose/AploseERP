package fr.aplose.erp.modules.agenda.repository;

import fr.aplose.erp.modules.agenda.entity.AgendaEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendaEventRepository extends JpaRepository<AgendaEvent, Long> {

    Optional<AgendaEvent> findByIdAndTenantId(Long id, String tenantId);

    @Query("SELECT e FROM AgendaEvent e WHERE e.tenantId = :tid " +
           "AND e.startDatetime >= :from AND e.startDatetime < :to " +
           "ORDER BY e.startDatetime ASC")
    List<AgendaEvent> findBetween(@Param("tid") String tenantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    Page<AgendaEvent> findByTenantIdAndStartDatetimeBetweenOrderByStartDatetimeAsc(
            String tenantId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
