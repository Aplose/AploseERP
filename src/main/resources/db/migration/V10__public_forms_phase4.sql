-- ============================================================
-- V10: Phase 4 – Formulaires publics (demande devis, contact)
-- ============================================================

CREATE TABLE public_forms (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        VARCHAR(36)  NOT NULL,
    code             VARCHAR(64)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(500) NULL,
    fields_json      TEXT         NOT NULL,
    success_message  VARCHAR(500) NULL,
    notify_emails    VARCHAR(500) NULL,
    captcha_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_public_forms PRIMARY KEY (id),
    CONSTRAINT fk_pf_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT uq_public_forms_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_public_forms_tenant ON public_forms(tenant_id);
CREATE INDEX idx_public_forms_tenant_enabled ON public_forms(tenant_id, enabled);

CREATE TABLE public_form_submissions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    form_id     BIGINT       NOT NULL,
    tenant_id   VARCHAR(36)  NOT NULL,
    data_json   TEXT         NOT NULL,
    ip_address  VARCHAR(45)  NULL,
    submitted_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified_at TIMESTAMP   NULL,
    CONSTRAINT pk_public_form_submissions PRIMARY KEY (id),
    CONSTRAINT fk_pfs_form FOREIGN KEY (form_id) REFERENCES public_forms(id) ON DELETE CASCADE,
    CONSTRAINT fk_pfs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX idx_public_form_submissions_form ON public_form_submissions(form_id);
CREATE INDEX idx_public_form_submissions_tenant ON public_form_submissions(tenant_id);

-- Permissions
INSERT INTO permissions (code, module, action, description) VALUES
  ('PUBLIC_FORM_READ', 'PUBLIC_FORM', 'READ', 'View public forms and submissions'),
  ('PUBLIC_FORM_CREATE', 'PUBLIC_FORM', 'CREATE', 'Create public forms'),
  ('PUBLIC_FORM_UPDATE', 'PUBLIC_FORM', 'UPDATE', 'Edit public forms'),
  ('PUBLIC_FORM_DELETE', 'PUBLIC_FORM', 'DELETE', 'Delete public forms');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('PUBLIC_FORM_READ', 'PUBLIC_FORM_CREATE', 'PUBLIC_FORM_UPDATE', 'PUBLIC_FORM_DELETE');
