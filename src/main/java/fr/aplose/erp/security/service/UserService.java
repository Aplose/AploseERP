package fr.aplose.erp.security.service;

import fr.aplose.erp.security.entity.Role;
import fr.aplose.erp.security.entity.User;
import fr.aplose.erp.security.repository.RoleRepository;
import fr.aplose.erp.security.repository.UserRepository;
import fr.aplose.erp.security.web.dto.ChangePasswordDto;
import fr.aplose.erp.security.web.dto.UserCreateDto;
import fr.aplose.erp.security.web.dto.UserEditDto;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findByTenantIdAndDeletedAtIsNull(TenantContext.getCurrentTenantId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> search(String query, Pageable pageable) {
        return userRepository.search(TenantContext.getCurrentTenantId(), query, pageable);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public User create(UserCreateDto dto) {
        String tenantId = TenantContext.getCurrentTenantId();

        if (userRepository.findByUsernameAndTenantIdAndDeletedAtIsNull(dto.getUsername(), tenantId).isPresent()) {
            throw new IllegalStateException("Username already taken: " + dto.getUsername());
        }
        if (userRepository.findByEmailAndTenantIdAndDeletedAtIsNull(dto.getEmail(), tenantId).isPresent()) {
            throw new IllegalStateException("Email already in use: " + dto.getEmail());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(dto.getUsername().trim().toLowerCase());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setLocale(dto.getLocale());
        user.setTimezone(dto.getTimezone());
        user.setTenantAdmin(dto.isTenantAdmin());
        user.setActive(true);
        user.setRoles(resolveRoles(dto.getRoleIds(), tenantId));

        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, UserEditDto dto) {
        User user = findById(id);

        String tenantId = TenantContext.getCurrentTenantId();
        userRepository.findByEmailAndTenantIdAndDeletedAtIsNull(dto.getEmail(), tenantId)
                .filter(u -> !u.getId().equals(id))
                .ifPresent(u -> { throw new IllegalStateException("Email already in use: " + dto.getEmail()); });

        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setLocale(dto.getLocale());
        user.setTimezone(dto.getTimezone());
        user.setActive(dto.isActive());
        user.setTenantAdmin(dto.isTenantAdmin());
        user.setRoles(resolveRoles(dto.getRoleIds(), tenantId));

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordDto dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void softDelete(Long id) {
        User user = findById(id);
        user.setDeletedAt(LocalDateTime.now());
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void toggleActive(Long id) {
        User user = findById(id);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    private Set<Role> resolveRoles(Set<Long> roleIds, String tenantId) {
        Set<Role> roles = new HashSet<>();
        if (roleIds != null) {
            roleIds.forEach(roleId ->
                roleRepository.findByIdAndTenantId(roleId, tenantId)
                        .ifPresent(roles::add)
            );
        }
        return roles;
    }
}
