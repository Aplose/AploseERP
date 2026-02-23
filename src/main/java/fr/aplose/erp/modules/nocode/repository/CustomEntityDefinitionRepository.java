package fr.aplose.erp.modules.nocode.repository;

import fr.aplose.erp.modules.nocode.entity.CustomEntityDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomEntityDefinitionRepository extends JpaRepository<CustomEntityDefinition, Long> {

    List<CustomEntityDefinition> findByModuleDefinitionIdOrderByCode(Long moduleDefinitionId);

    Optional<CustomEntityDefinition> findByModuleDefinitionIdAndCode(Long moduleDefinitionId, String code);
}
