package fr.aplose.erp.security.repository;

import fr.aplose.erp.security.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByKeyPrefix(String keyPrefix);

    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<ApiKey> findByIdAndTenantId(Long id, String tenantId);
}
