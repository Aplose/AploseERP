package fr.aplose.erp.security.repository;

import fr.aplose.erp.security.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByModuleOrderByActionAsc(String module);

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByOrderByModuleAscActionAsc();
}
