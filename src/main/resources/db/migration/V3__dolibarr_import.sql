-- ============================================================
-- V3: Dolibarr import (runs, logs, mapping, config, staging)
-- ============================================================

CREATE TABLE dolibarr_import_config (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)  NOT NULL,
    base_url       VARCHAR(500) NOT NULL,
    api_key_encrypted VARCHAR(500),
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dolibarr_import_config PRIMARY KEY (id),
    CONSTRAINT fk_dic_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_dic_tenant UNIQUE (tenant_id)
);

CREATE TABLE dolibarr_import_run (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id           VARCHAR(36)  NOT NULL,
    started_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at         TIMESTAMP    NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'RUNNING',
    dolibarr_base_url   VARCHAR(500),
    config_id           BIGINT       NULL,
    created_by          BIGINT       NULL,
    CONSTRAINT pk_dolibarr_import_run PRIMARY KEY (id),
    CONSTRAINT fk_dir_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_dir_config FOREIGN KEY (config_id) REFERENCES dolibarr_import_config(id) ON DELETE SET NULL,
    CONSTRAINT fk_dir_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE dolibarr_import_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    import_run_id  BIGINT       NOT NULL,
    step           VARCHAR(80)  NOT NULL,
    level          VARCHAR(10)  NOT NULL,
    external_id    VARCHAR(50)  NULL,
    entity_type    VARCHAR(50)  NULL,
    entity_id      BIGINT       NULL,
    message        VARCHAR(1000) NOT NULL,
    detail_json    TEXT         NULL,
    CONSTRAINT pk_dolibarr_import_log PRIMARY KEY (id),
    CONSTRAINT fk_dil_run FOREIGN KEY (import_run_id) REFERENCES dolibarr_import_run(id) ON DELETE CASCADE
);

CREATE TABLE dolibarr_import_mapping (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)  NOT NULL,
    import_run_id   BIGINT       NOT NULL,
    dolibarr_entity VARCHAR(50)  NOT NULL,
    dolibarr_id     BIGINT       NOT NULL,
    aplose_entity   VARCHAR(50)  NOT NULL,
    aplose_id       BIGINT       NOT NULL,
    CONSTRAINT pk_dolibarr_import_mapping PRIMARY KEY (id),
    CONSTRAINT fk_dim_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_dim_run FOREIGN KEY (import_run_id) REFERENCES dolibarr_import_run(id) ON DELETE CASCADE
);

CREATE TABLE dolibarr_import_staging (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    import_run_id    BIGINT       NOT NULL,
    entity_dolibarr  VARCHAR(50)  NOT NULL,
    external_id      BIGINT       NOT NULL,
    payload_json     TEXT         NOT NULL,
    CONSTRAINT pk_dolibarr_import_staging PRIMARY KEY (id),
    CONSTRAINT fk_dis_run FOREIGN KEY (import_run_id) REFERENCES dolibarr_import_run(id) ON DELETE CASCADE
);

CREATE INDEX idx_dir_tenant ON dolibarr_import_run(tenant_id);
CREATE INDEX idx_dir_started ON dolibarr_import_run(started_at DESC);
CREATE INDEX idx_dil_run ON dolibarr_import_log(import_run_id);
CREATE INDEX idx_dil_level ON dolibarr_import_log(import_run_id, level);
CREATE INDEX idx_dim_tenant_entity ON dolibarr_import_mapping(tenant_id, dolibarr_entity, dolibarr_id);
CREATE INDEX idx_dim_run ON dolibarr_import_mapping(import_run_id);
CREATE INDEX idx_dis_run ON dolibarr_import_staging(import_run_id);

INSERT INTO permissions (code, module, action, description) VALUES
  ('DOLIBARR_IMPORT', 'ADMIN', 'IMPORT', 'Import data from Dolibarr');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code = 'DOLIBARR_IMPORT';
