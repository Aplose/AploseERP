-- ============================================================
-- V2: Bank, Accounting, GED, Ticketing (Phase 1 modules)
-- All tables are tenant-scoped (tenant_id) for strict isolation.
-- ============================================================

-- ---------------------------------------------------------------------------
-- Bank: accounts and movements
-- ---------------------------------------------------------------------------
CREATE TABLE bank_accounts (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)  NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    iban           VARCHAR(34),
    bic            VARCHAR(11),
    currency_code  CHAR(3)       NOT NULL DEFAULT 'EUR',
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bank_accounts PRIMARY KEY (id),
    CONSTRAINT fk_ba_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ba_currency FOREIGN KEY (currency_code) REFERENCES currencies(code)
);

CREATE TABLE bank_movements (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)   NOT NULL,
    account_id     BIGINT        NOT NULL,
    movement_date  DATE          NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    currency_code  CHAR(3)       NOT NULL,
    description    VARCHAR(500),
    reference      VARCHAR(100),
    payment_id     BIGINT,
    invoice_id     BIGINT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT,
    CONSTRAINT pk_bank_movements PRIMARY KEY (id),
    CONSTRAINT fk_bm_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants(id),
    CONSTRAINT fk_bm_account  FOREIGN KEY (account_id)  REFERENCES bank_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_bm_currency FOREIGN KEY (currency_code) REFERENCES currencies(code),
    CONSTRAINT fk_bm_payment  FOREIGN KEY (payment_id)  REFERENCES payments(id) ON DELETE SET NULL,
    CONSTRAINT fk_bm_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices(id) ON DELETE SET NULL,
    CONSTRAINT fk_bm_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_bank_accounts_tenant ON bank_accounts(tenant_id);
CREATE INDEX idx_bank_movements_tenant ON bank_movements(tenant_id);
CREATE INDEX idx_bank_movements_account_date ON bank_movements(account_id, movement_date);

-- ---------------------------------------------------------------------------
-- Accounting: chart of accounts, journals, entries
-- ---------------------------------------------------------------------------
CREATE TABLE accounting_accounts (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    account_type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    parent_id   BIGINT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_accounting_accounts PRIMARY KEY (id),
    CONSTRAINT fk_aa_tenant  FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_aa_parent FOREIGN KEY (parent_id) REFERENCES accounting_accounts(id),
    CONSTRAINT uq_accounting_accounts_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE accounting_journals (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_accounting_journals PRIMARY KEY (id),
    CONSTRAINT fk_aj_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_accounting_journals_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE accounting_entries (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(36)  NOT NULL,
    journal_id   BIGINT       NOT NULL,
    entry_date   DATE         NOT NULL,
    reference    VARCHAR(100),
    description  VARCHAR(500),
    validated_at TIMESTAMP,
    validated_by BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   BIGINT,
    CONSTRAINT pk_accounting_entries PRIMARY KEY (id),
    CONSTRAINT fk_accent_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants(id),
    CONSTRAINT fk_accent_journal FOREIGN KEY (journal_id)  REFERENCES accounting_journals(id),
    CONSTRAINT fk_accent_validated_by FOREIGN KEY (validated_by) REFERENCES users(id),
    CONSTRAINT fk_accent_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE accounting_entry_lines (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)   NOT NULL,
    entry_id        BIGINT        NOT NULL,
    account_id      BIGINT        NOT NULL,
    debit           NUMERIC(19,4) NOT NULL DEFAULT 0,
    credit          NUMERIC(19,4) NOT NULL DEFAULT 0,
    line_description VARCHAR(500),
    sort_order      SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_accounting_entry_lines PRIMARY KEY (id),
    CONSTRAINT fk_ael_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants(id),
    CONSTRAINT fk_ael_entry   FOREIGN KEY (entry_id)   REFERENCES accounting_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_ael_account FOREIGN KEY (account_id)  REFERENCES accounting_accounts(id)
);

CREATE INDEX idx_accounting_accounts_tenant ON accounting_accounts(tenant_id);
CREATE INDEX idx_accounting_journals_tenant ON accounting_journals(tenant_id);
CREATE INDEX idx_accounting_entries_tenant ON accounting_entries(tenant_id);
CREATE INDEX idx_accounting_entries_journal_date ON accounting_entries(journal_id, entry_date);
CREATE INDEX idx_accounting_entry_lines_entry ON accounting_entry_lines(entry_id);

-- ---------------------------------------------------------------------------
-- GED: documents (tenant + entity type + entity id)
-- ---------------------------------------------------------------------------
CREATE TABLE ged_documents (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(36)  NOT NULL,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    file_path    VARCHAR(500) NOT NULL,
    mime_type    VARCHAR(100),
    file_size    BIGINT,
    version      SMALLINT     NOT NULL DEFAULT 1,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   BIGINT,
    CONSTRAINT pk_ged_documents PRIMARY KEY (id),
    CONSTRAINT fk_ged_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ged_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_ged_documents_tenant ON ged_documents(tenant_id);
CREATE INDEX idx_ged_documents_entity ON ged_documents(tenant_id, entity_type, entity_id);

-- ---------------------------------------------------------------------------
-- Ticketing: tickets and comments
-- ---------------------------------------------------------------------------
CREATE TABLE tickets (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(36)  NOT NULL,
    subject      VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    requester_id BIGINT,
    assignee_id  BIGINT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tickets PRIMARY KEY (id),
    CONSTRAINT fk_tickets_tenant     FOREIGN KEY (tenant_id)     REFERENCES tenants(id),
    CONSTRAINT fk_tickets_requester  FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_assignee   FOREIGN KEY (assignee_id)   REFERENCES users(id)
);

CREATE TABLE ticket_comments (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id  VARCHAR(36)  NOT NULL,
    ticket_id  BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ticket_comments PRIMARY KEY (id),
    CONSTRAINT fk_tc_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants(id),
    CONSTRAINT fk_tc_ticket   FOREIGN KEY (ticket_id)  REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_tc_user    FOREIGN KEY (user_id)    REFERENCES users(id)
);

CREATE INDEX idx_tickets_tenant ON tickets(tenant_id);
CREATE INDEX idx_tickets_status ON tickets(tenant_id, status);
CREATE INDEX idx_ticket_comments_ticket ON ticket_comments(ticket_id);

-- Permissions for new modules (CRUD)
INSERT INTO permissions (code, module, action, description) VALUES
  ('BANK_CREATE', 'BANK', 'CREATE', 'Create bank accounts and movements'),
  ('BANK_UPDATE', 'BANK', 'UPDATE', 'Edit bank data'),
  ('BANK_DELETE', 'BANK', 'DELETE', 'Delete bank data'),
  ('ACCOUNTING_CREATE', 'ACCOUNTING', 'CREATE', 'Create accounting entries'),
  ('ACCOUNTING_UPDATE', 'ACCOUNTING', 'UPDATE', 'Edit accounting data'),
  ('ACCOUNTING_DELETE', 'ACCOUNTING', 'DELETE', 'Delete accounting data'),
  ('GED_CREATE', 'GED', 'CREATE', 'Upload documents'),
  ('GED_UPDATE', 'GED', 'UPDATE', 'Edit document metadata'),
  ('GED_DELETE', 'GED', 'DELETE', 'Delete documents'),
  ('TICKETING_CREATE', 'TICKETING', 'CREATE', 'Create tickets'),
  ('TICKETING_UPDATE', 'TICKETING', 'UPDATE', 'Edit and assign tickets'),
  ('TICKETING_DELETE', 'TICKETING', 'DELETE', 'Delete tickets');

-- Grant new permissions to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('BANK_CREATE','BANK_UPDATE','BANK_DELETE','ACCOUNTING_CREATE','ACCOUNTING_UPDATE','ACCOUNTING_DELETE','GED_CREATE','GED_UPDATE','GED_DELETE','TICKETING_CREATE','TICKETING_UPDATE','TICKETING_DELETE');

-- Demo tenant: accounting journals and sample chart of accounts
INSERT INTO accounting_journals (tenant_id, code, label, is_active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'BQ', 'Bank', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000001', 'VT', 'Sales', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000001', 'ACH', 'Purchases', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000001', 'OD', 'Miscellaneous', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO accounting_accounts (tenant_id, code, label, account_type, is_active, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001', '512', 'Bank account', 'GENERAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000001', '411', 'Customers', 'GENERAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000001', '401', 'Suppliers', 'GENERAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000001', '706', 'Sales', 'GENERAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('00000000-0000-0000-0000-000000000001', '606', 'Purchases', 'GENERAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
