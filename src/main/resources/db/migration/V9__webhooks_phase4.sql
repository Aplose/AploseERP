-- ============================================================
-- V9: Phase 4 – Webhooks (émission d’événements vers URLs)
-- ============================================================

CREATE TABLE webhook_endpoints (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    url         VARCHAR(500) NOT NULL,
    secret      VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    event_types VARCHAR(500) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_webhook_endpoints PRIMARY KEY (id),
    CONSTRAINT fk_we_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX idx_webhook_endpoints_tenant ON webhook_endpoints(tenant_id);

-- Permissions
INSERT INTO permissions (code, module, action, description) VALUES
  ('WEBHOOK_READ', 'WEBHOOK', 'READ', 'View webhook endpoints'),
  ('WEBHOOK_CREATE', 'WEBHOOK', 'CREATE', 'Create webhook endpoints'),
  ('WEBHOOK_UPDATE', 'WEBHOOK', 'UPDATE', 'Edit webhook endpoints'),
  ('WEBHOOK_DELETE', 'WEBHOOK', 'DELETE', 'Delete webhook endpoints');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('WEBHOOK_READ', 'WEBHOOK_CREATE', 'WEBHOOK_UPDATE', 'WEBHOOK_DELETE');
