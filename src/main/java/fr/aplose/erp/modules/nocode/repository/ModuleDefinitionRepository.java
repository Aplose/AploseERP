package fr.aplose.erp.modules.nocode.repository;

import fr.aplose.erp.modules.nocode.entity.ModuleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuleDefinitionRepository extends JpaRepository<ModuleDefinition, Long> {

    List<ModuleDefinition> findByIsPublicTrueOrderByNameAsc();

    Optional<ModuleDefinition> findByCodeAndVersion(String code, String version);

    Optional<ModuleDefinition> findFirstByCodeOrderByUpdatedAtDesc(String code);

    List<ModuleDefinition> findByAuthorTenantIdOrderByNameAsc(String authorTenantId);
}
