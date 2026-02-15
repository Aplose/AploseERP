package fr.aplose.erp.security.repository;

import fr.aplose.erp.security.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByTenantId(String tenantId);

    Optional<Role> findByCodeAndTenantId(String code, String tenantId);

    Optional<Role> findByIdAndTenantId(Long id, String tenantId);
}
