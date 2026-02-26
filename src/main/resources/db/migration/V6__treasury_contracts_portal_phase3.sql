-- ============================================================
-- V6: Phase 3 – Trésorerie, contrats commerciaux, portail client
-- ============================================================

-- ---------------------------------------------------------------------------
-- Contrats commerciaux (tiers, type, dates, renouvellement)
-- ---------------------------------------------------------------------------
CREATE TABLE business_contracts (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)  NOT NULL,
    third_party_id  BIGINT       NOT NULL,
    contact_id      BIGINT,
    contract_type   VARCHAR(50)  NOT NULL,
    reference       VARCHAR(50),
    start_date      DATE         NOT NULL,
    end_date        DATE,
    renewal_type    VARCHAR(30),
    renewal_notice_days INT,
    proposal_id     BIGINT,
    status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    notes           TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_business_contracts PRIMARY KEY (id),
    CONSTRAINT fk_bc_tenant    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_bc_third_party FOREIGN KEY (third_party_id) REFERENCES third_parties(id) ON DELETE CASCADE,
    CONSTRAINT fk_bc_contact   FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL,
    CONSTRAINT fk_bc_proposal  FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE SET NULL
);

CREATE INDEX idx_business_contracts_tenant ON business_contracts(tenant_id);
CREATE INDEX idx_business_contracts_third_party ON business_contracts(third_party_id);

-- ---------------------------------------------------------------------------
-- Portail client : lien utilisateur → tiers
-- ---------------------------------------------------------------------------
ALTER TABLE users ADD COLUMN third_party_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_third_party FOREIGN KEY (third_party_id) REFERENCES third_parties(id) ON DELETE SET NULL;
CREATE INDEX idx_users_third_party ON users(third_party_id);

-- ---------------------------------------------------------------------------
-- Signature électronique : statut sur les devis
-- ---------------------------------------------------------------------------
ALTER TABLE proposals ADD COLUMN signature_status VARCHAR(30) NULL;
ALTER TABLE proposals ADD COLUMN signature_external_id VARCHAR(255) NULL;

-- ---------------------------------------------------------------------------
-- Permissions
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, module, action, description) VALUES
  ('TREASURY_READ', 'TREASURY', 'READ', 'View treasury and cash flow'),
  ('BUSINESS_CONTRACT_READ', 'BUSINESS_CONTRACT', 'READ', 'View business contracts'),
  ('BUSINESS_CONTRACT_CREATE', 'BUSINESS_CONTRACT', 'CREATE', 'Create business contracts'),
  ('BUSINESS_CONTRACT_UPDATE', 'BUSINESS_CONTRACT', 'UPDATE', 'Edit business contracts'),
  ('BUSINESS_CONTRACT_DELETE', 'BUSINESS_CONTRACT', 'DELETE', 'Delete business contracts'),
  ('PORTAL_ACCESS', 'PORTAL', 'ACCESS', 'Access client portal'),
  ('SIGNATURE_SEND', 'SIGNATURE', 'SEND', 'Send document for electronic signature');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('TREASURY_READ', 'BUSINESS_CONTRACT_READ', 'BUSINESS_CONTRACT_CREATE', 'BUSINESS_CONTRACT_UPDATE', 'BUSINESS_CONTRACT_DELETE', 'PORTAL_ACCESS', 'SIGNATURE_SEND');

-- Enable modules for existing tenants
INSERT INTO tenant_modules (tenant_id, module_code, enabled)
SELECT t.id, 'TREASURY', TRUE FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM tenant_modules tm WHERE tm.tenant_id = t.id AND tm.module_code = 'TREASURY');
INSERT INTO tenant_modules (tenant_id, module_code, enabled)
SELECT t.id, 'BUSINESS_CONTRACT', TRUE FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM tenant_modules tm WHERE tm.tenant_id = t.id AND tm.module_code = 'BUSINESS_CONTRACT');
