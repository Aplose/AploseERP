package fr.aplose.erp.dolibarr.repository;

import fr.aplose.erp.dolibarr.entity.DolibarrImportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DolibarrImportConfigRepository extends JpaRepository<DolibarrImportConfig, Long> {

    Optional<DolibarrImportConfig> findByTenantId(String tenantId);
}
