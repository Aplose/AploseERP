package fr.aplose.erp.security.service;

import fr.aplose.erp.security.entity.Permission;
import fr.aplose.erp.security.entity.Role;
import fr.aplose.erp.security.repository.PermissionRepository;
import fr.aplose.erp.security.repository.RoleRepository;
import fr.aplose.erp.security.web.dto.RoleDto;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findByTenantId(TenantContext.getCurrentTenantId());
    }

    @Transactional(readOnly = true)
    public Role findById(Long id) {
        return roleRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));
    }

    @Transactional(readOnly = true)
    public Map<String, List<Permission>> permissionsGroupedByModule() {
        return permissionRepository.findAllByOrderByModuleAscActionAsc()
                .stream()
                .collect(Collectors.groupingBy(Permission::getModule, LinkedHashMap::new, Collectors.toList()));
    }

    @Transactional
    public Role create(RoleDto dto) {
        String tenantId = TenantContext.getCurrentTenantId();

        roleRepository.findByCodeAndTenantId(dto.getCode(), tenantId)
                .ifPresent(r -> { throw new IllegalStateException("Role code already exists: " + dto.getCode()); });

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setCode(dto.getCode().toUpperCase());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setSystem(false);
        role.setPermissions(resolvePermissions(dto.getPermissionIds()));

        return roleRepository.save(role);
    }

    @Transactional
    public Role update(Long id, RoleDto dto) {
        Role role = findById(id);

        if (role.isSystem()) {
            throw new IllegalStateException("Cannot modify a system role");
        }

        // Check code uniqueness (excluding self)
        roleRepository.findByCodeAndTenantId(dto.getCode().toUpperCase(), role.getTenantId())
                .filter(r -> !r.getId().equals(id))
                .ifPresent(r -> { throw new IllegalStateException("Role code already exists: " + dto.getCode()); });

        role.setCode(dto.getCode().toUpperCase());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setPermissions(resolvePermissions(dto.getPermissionIds()));

        return roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = findById(id);
        if (role.isSystem()) {
            throw new IllegalStateException("Cannot delete a system role");
        }
        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(permissionRepository.findAllById(ids));
    }
}
