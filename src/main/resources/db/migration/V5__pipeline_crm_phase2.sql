-- ============================================================
-- V5: Phase 2 – Pipeline commercial, activités CRM, relances
-- ============================================================

-- ---------------------------------------------------------------------------
-- Pipeline commercial : étapes de vente
-- ---------------------------------------------------------------------------
CREATE TABLE sales_pipeline_stages (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    probability NUMERIC(5,2) NOT NULL DEFAULT 0,
    is_closed   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sales_pipeline_stages PRIMARY KEY (id),
    CONSTRAINT fk_sps_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_sales_pipeline_stages_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_sales_pipeline_stages_tenant ON sales_pipeline_stages(tenant_id);

ALTER TABLE proposals ADD COLUMN pipeline_stage_id BIGINT NULL;
ALTER TABLE proposals ADD CONSTRAINT fk_proposals_pipeline_stage
    FOREIGN KEY (pipeline_stage_id) REFERENCES sales_pipeline_stages(id) ON DELETE SET NULL;
CREATE INDEX idx_proposals_pipeline_stage ON proposals(pipeline_stage_id);

-- ---------------------------------------------------------------------------
-- Activités CRM : tâches liées à un tiers/contact
-- ---------------------------------------------------------------------------
CREATE TABLE crm_activities (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        VARCHAR(36)  NOT NULL,
    third_party_id   BIGINT       NOT NULL,
    contact_id       BIGINT,
    activity_type    VARCHAR(30)  NOT NULL,
    subject          VARCHAR(500) NOT NULL,
    description      TEXT,
    due_date         DATE,
    due_time         TIME,
    completed_at     TIMESTAMP,
    assigned_to_id   BIGINT,
    agenda_event_id  BIGINT,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT,
    CONSTRAINT pk_crm_activities PRIMARY KEY (id),
    CONSTRAINT fk_cra_tenant     FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_cra_third_party FOREIGN KEY (third_party_id) REFERENCES third_parties(id) ON DELETE CASCADE,
    CONSTRAINT fk_cra_contact    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL,
    CONSTRAINT fk_cra_assigned   FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_cra_agenda     FOREIGN KEY (agenda_event_id) REFERENCES agenda_events(id) ON DELETE SET NULL,
    CONSTRAINT fk_cra_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_crm_activities_tenant ON crm_activities(tenant_id);
CREATE INDEX idx_crm_activities_third_party ON crm_activities(third_party_id);
CREATE INDEX idx_crm_activities_due ON crm_activities(due_date, completed_at);

-- ---------------------------------------------------------------------------
-- Relances : suivi des relances (optionnel, pour marquer "relancé le")
-- ---------------------------------------------------------------------------
ALTER TABLE proposals ADD COLUMN last_reminder_at TIMESTAMP NULL;
ALTER TABLE invoices ADD COLUMN last_reminder_at TIMESTAMP NULL;

-- Permissions
INSERT INTO permissions (code, module, action, description) VALUES
  ('PIPELINE_READ', 'PIPELINE', 'READ', 'View sales pipeline'),
  ('PIPELINE_UPDATE', 'PIPELINE', 'UPDATE', 'Move proposals in pipeline'),
  ('CRM_ACTIVITY_CREATE', 'CRM_ACTIVITY', 'CREATE', 'Create CRM activities'),
  ('CRM_ACTIVITY_READ', 'CRM_ACTIVITY', 'READ', 'View CRM activities'),
  ('CRM_ACTIVITY_UPDATE', 'CRM_ACTIVITY', 'UPDATE', 'Edit CRM activities'),
  ('CRM_ACTIVITY_DELETE', 'CRM_ACTIVITY', 'DELETE', 'Delete CRM activities'),
  ('FOLLOW_UP_READ', 'FOLLOW_UP', 'READ', 'View follow-up list');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('PIPELINE_READ', 'PIPELINE_UPDATE', 'CRM_ACTIVITY_CREATE', 'CRM_ACTIVITY_READ', 'CRM_ACTIVITY_UPDATE', 'CRM_ACTIVITY_DELETE', 'FOLLOW_UP_READ');

-- Enable Pipeline and CRM Activity modules for all existing tenants
INSERT INTO tenant_modules (tenant_id, module_code, enabled)
SELECT t.id, 'PIPELINE', TRUE FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM tenant_modules tm WHERE tm.tenant_id = t.id AND tm.module_code = 'PIPELINE');
INSERT INTO tenant_modules (tenant_id, module_code, enabled)
SELECT t.id, 'CRM_ACTIVITY', TRUE FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM tenant_modules tm WHERE tm.tenant_id = t.id AND tm.module_code = 'CRM_ACTIVITY');
INSERT INTO tenant_modules (tenant_id, module_code, enabled)
SELECT t.id, 'FOLLOW_UP', TRUE FROM tenants t
WHERE NOT EXISTS (SELECT 1 FROM tenant_modules tm WHERE tm.tenant_id = t.id AND tm.module_code = 'FOLLOW_UP');

-- Default pipeline stages for demo tenant
INSERT INTO sales_pipeline_stages (tenant_id, code, label, sort_order, probability, is_closed)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'QUALIFICATION', 'Qualification', 10, 10, FALSE),
  ('00000000-0000-0000-0000-000000000001', 'QUOTE_SENT', 'Devis envoyé', 20, 25, FALSE),
  ('00000000-0000-0000-0000-000000000001', 'NEGOTIATION', 'Négociation', 30, 50, FALSE),
  ('00000000-0000-0000-0000-000000000001', 'WON', 'Gagné', 40, 100, TRUE),
  ('00000000-0000-0000-0000-000000000001', 'LOST', 'Perdu', 50, 0, TRUE);
