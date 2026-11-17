package fr.aplose.erp.security.repository;

import fr.aplose.erp.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndTenantIdAndDeletedAtIsNull(String username, String tenantId);

    Optional<User> findByEmailAndTenantIdAndDeletedAtIsNull(String email, String tenantId);

    Optional<User> findByIdAndTenantId(Long id, String tenantId);

    /**
     * Users matching a login identifier (email or username), all companies included.
     * There is a single login page for every company, so identifiers are unique platform-wide.
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (LOWER(u.email) = LOWER(:login) OR LOWER(u.username) = LOWER(:login))")
    java.util.List<User> findAllByLogin(@Param("login") String login);

    /** True if the identifier is already the email or username of another (non-deleted) user, in any company. */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.deletedAt IS NULL " +
           "AND (LOWER(u.email) = LOWER(:value) OR LOWER(u.username) = LOWER(:value)) " +
           "AND (:excludeUserId IS NULL OR u.id <> :excludeUserId)")
    boolean isLoginTaken(@Param("value") String value, @Param("excludeUserId") Long excludeUserId);

    Page<User> findByTenantIdAndDeletedAtIsNull(String tenantId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<User> search(@Param("tenantId") String tenantId, @Param("q") String query, Pageable pageable);
}
