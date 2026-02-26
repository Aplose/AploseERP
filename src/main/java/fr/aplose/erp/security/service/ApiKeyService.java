package fr.aplose.erp.security.service;

import fr.aplose.erp.security.entity.ApiKey;
import fr.aplose.erp.security.repository.ApiKeyRepository;
import fr.aplose.erp.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private static final String PREFIX = "ak_";
    private static final int KEY_BYTES = 32;
    private static final int PREFIX_LENGTH = 8; // PREFIX + 5 hex chars for lookup

    private final ApiKeyRepository repository;

    /**
     * Creates a new API key. The raw key is returned only once and cannot be retrieved later.
     */
    @Transactional
    public String create(String tenantId, Long userId, String name) {
        SecureRandom rng = new SecureRandom();
        byte[] bytes = new byte[KEY_BYTES];
        rng.nextBytes(bytes);
        String rawKey = PREFIX + HexFormat.of().formatHex(bytes);
        String keyPrefix = rawKey.length() >= PREFIX_LENGTH ? rawKey.substring(0, PREFIX_LENGTH) : rawKey;
        String keyHash = hash(rawKey);

        ApiKey key = new ApiKey();
        key.setTenantId(tenantId);
        key.setUserId(userId);
        key.setName(name);
        key.setKeyPrefix(keyPrefix);
        key.setKeyHash(keyHash);
        key.setCreatedAt(LocalDateTime.now());
        repository.save(key);
        log.info("API key created for tenant={} user={} name={}", tenantId, userId, name);
        return rawKey;
    }

    /**
     * Validates the raw key and returns the associated tenant id and user id if valid.
     */
    @Transactional
    public Optional<ApiKeyAuthResult> validate(String rawKey) {
        if (rawKey == null || rawKey.length() < PREFIX_LENGTH || !rawKey.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String keyPrefix = rawKey.substring(0, PREFIX_LENGTH);
        String keyHash = hash(rawKey);

        List<ApiKey> candidates = repository.findByKeyPrefix(keyPrefix);
        for (ApiKey key : candidates) {
            if (key.getKeyHash().equals(keyHash) && !key.isExpired()) {
                key.setLastUsedAt(LocalDateTime.now());
                repository.save(key);
                return Optional.of(new ApiKeyAuthResult(key.getTenantId(), key.getUserId(), key.getId()));
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<ApiKey> findByTenant() {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    @Transactional
    public void revoke(Long id) {
        ApiKey key = repository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));
        repository.delete(key);
        log.info("API key revoked: id={}", id);
    }

    private static String hash(String rawKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record ApiKeyAuthResult(String tenantId, Long userId, Long keyId) {}
}
