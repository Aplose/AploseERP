-- ============================================================
-- V4: HR module – job positions, employees, contracts
-- All tables tenant-scoped (tenant_id).
-- ============================================================

-- Job positions (postes)
CREATE TABLE hr_job_positions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    department  VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_hr_job_positions PRIMARY KEY (id),
    CONSTRAINT fk_hr_jp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_hr_job_positions_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_hr_job_positions_tenant ON hr_job_positions(tenant_id);

-- Employees (fiches salariés) – linked to User and/or Contact
CREATE TABLE hr_employees (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id         VARCHAR(36)  NOT NULL,
    user_id           BIGINT,
    contact_id        BIGINT,
    employee_number   VARCHAR(50),
    job_position_id   BIGINT,
    manager_id        BIGINT,
    hire_date         DATE,
    termination_date  DATE,
    status            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_hr_employees PRIMARY KEY (id),
    CONSTRAINT fk_hr_emp_tenant   FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_hr_emp_user     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_hr_emp_contact   FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL,
    CONSTRAINT fk_hr_emp_position FOREIGN KEY (job_position_id) REFERENCES hr_job_positions(id) ON DELETE SET NULL,
    CONSTRAINT fk_hr_emp_manager  FOREIGN KEY (manager_id) REFERENCES hr_employees(id) ON DELETE SET NULL
);

CREATE INDEX idx_hr_employees_tenant ON hr_employees(tenant_id);
CREATE INDEX idx_hr_employees_user ON hr_employees(user_id);
CREATE INDEX idx_hr_employees_manager ON hr_employees(manager_id);

-- Contracts (contrats)
CREATE TABLE hr_contracts (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)  NOT NULL,
    employee_id    BIGINT       NOT NULL,
    contract_type  VARCHAR(30)  NOT NULL,
    start_date     DATE         NOT NULL,
    end_date       DATE,
    status         VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    notes          TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_hr_contracts PRIMARY KEY (id),
    CONSTRAINT fk_hr_contract_tenant   FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_hr_contract_employee FOREIGN KEY (employee_id) REFERENCES hr_employees(id) ON DELETE CASCADE
);

CREATE INDEX idx_hr_contracts_tenant ON hr_contracts(tenant_id);
CREATE INDEX idx_hr_contracts_employee ON hr_contracts(employee_id);

-- Permissions HR
INSERT INTO permissions (code, module, action, description) VALUES
  ('HR_CREATE', 'HR', 'CREATE', 'Create HR data'),
  ('HR_UPDATE', 'HR', 'UPDATE', 'Edit HR data'),
  ('HR_DELETE', 'HR', 'DELETE', 'Delete HR data');

-- Grant new permissions to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code IN ('HR_CREATE', 'HR_UPDATE', 'HR_DELETE');
