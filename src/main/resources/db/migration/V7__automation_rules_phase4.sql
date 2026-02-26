-- ============================================================
-- V7: Phase 4 – Moteur de règles (automatisation)
-- ============================================================

-- ---------------------------------------------------------------------------
-- Règles d'automatisation : si (entité + événement + conditions) alors (action)
-- ---------------------------------------------------------------------------
CREATE TABLE automation_rules (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id               VARCHAR(36)  NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             VARCHAR(500),
    trigger_entity          VARCHAR(50)  NOT NULL,
    trigger_event           VARCHAR(50)  NOT NULL,
    condition_status        VARCHAR(50),
    condition_amount_min     DECIMAL(19,4),
    condition_amount_max     DECIMAL(19,4),
    condition_third_party_id BIGINT,
    action_type             VARCHAR(50)  NOT NULL,
    action_params           TEXT,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_automation_rules PRIMARY KEY (id),
    CONSTRAINT fk_ar_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ar_third_party FOREIGN KEY (condition_third_party_id) REFERENCES third_parties(id) ON DELETE SET NULL
);

CREATE INDEX idx_automation_rules_tenant ON automation_rules(tenant_id);
CREATE INDEX idx_automation_rules_trigger ON automation_rules(tenant_id, trigger_entity, trigger_event, enabled);

-- ---------------------------------------------------------------------------
-- Permissions
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, module, action, description) VALUES
  ('AUTOMATION_READ', 'AUTOMATION', 'READ', 'View automation rules'),
  ('AUTOMATION_CREATE', 'AUTOMATION', 'CREATE', 'Create automation rules'),
  ('AUTOMATION_UPDATE', 'AUTOMATION', 'UPDATE', 'Edit automation rules'),
  ('AUTOMATION_DELETE', 'AUTOMATION', 'DELETE', 'Delete automation rules');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('AUTOMATION_READ', 'AUTOMATION_CREATE', 'AUTOMATION_UPDATE', 'AUTOMATION_DELETE');

-- Enable module for existing tenants
INSERT INTO tenant_modules (tenant_id, module_code, enabled)
SELECT t.id, 'AUTOMATION', TRUE FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM tenant_modules tm WHERE tm.tenant_id = t.id AND tm.module_code = 'AUTOMATION');
