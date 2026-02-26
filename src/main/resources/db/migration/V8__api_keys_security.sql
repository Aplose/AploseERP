-- ============================================================
-- V8: API Keys – authentification avancée pour l’API REST
-- ============================================================

CREATE TABLE api_keys (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(36)  NOT NULL,
    user_id      BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    key_prefix   VARCHAR(16)  NOT NULL,
    key_hash     VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP    NULL,
    expires_at   TIMESTAMP    NULL,
    CONSTRAINT pk_api_keys PRIMARY KEY (id),
    CONSTRAINT fk_ak_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_ak_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix);
CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);

-- Permissions
INSERT INTO permissions (code, module, action, description) VALUES
  ('API_KEY_READ', 'API', 'READ', 'View API keys'),
  ('API_KEY_CREATE', 'API', 'CREATE', 'Create API keys'),
  ('API_KEY_DELETE', 'API', 'DELETE', 'Revoke API keys');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('API_KEY_READ', 'API_KEY_CREATE', 'API_KEY_DELETE');
