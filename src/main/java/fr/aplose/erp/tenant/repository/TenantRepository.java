package fr.aplose.erp.tenant.repository;

import fr.aplose.erp.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findByCode(String code);

    Optional<Tenant> findByCodeAndActiveTrue(String code);
}
