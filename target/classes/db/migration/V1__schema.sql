-- ============================================================
-- V1: Full schema (consolidated from V1–V18, base vierge)
-- ============================================================

-- ---------------------------------------------------------------------------
-- Tenants
-- ---------------------------------------------------------------------------
CREATE TABLE tenants (
    id               VARCHAR(36)  NOT NULL,
    code             VARCHAR(50)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    legal_name       VARCHAR(255),
    registration_id  VARCHAR(100),
    address_line1    VARCHAR(255),
    address_line2    VARCHAR(255),
    city             VARCHAR(100),
    state_province   VARCHAR(100),
    postal_code      VARCHAR(20),
    country_code     CHAR(2),
    phone            VARCHAR(50),
    email            VARCHAR(255),
    website          VARCHAR(255),
    default_locale   VARCHAR(10)  NOT NULL DEFAULT 'en',
    default_currency CHAR(3)      NOT NULL DEFAULT 'USD',
    fiscal_year_start SMALLINT    NOT NULL DEFAULT 1,
    logo_path        VARCHAR(500),
    timezone         VARCHAR(100) NOT NULL DEFAULT 'UTC',
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    plan             VARCHAR(50)  NOT NULL DEFAULT 'trial',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uq_tenants_code UNIQUE (code)
);

CREATE TABLE tenant_messages (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    locale      VARCHAR(10)  NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    message_val TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tenant_messages PRIMARY KEY (id),
    CONSTRAINT fk_tenant_messages_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_tenant_messages UNIQUE (tenant_id, locale, message_key)
);

CREATE TABLE number_sequences (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36) NOT NULL,
    module      VARCHAR(50) NOT NULL,
    prefix      VARCHAR(20),
    year_in_seq BOOLEAN     NOT NULL DEFAULT TRUE,
    last_number BIGINT      NOT NULL DEFAULT 0,
    padding     SMALLINT    NOT NULL DEFAULT 4,
    CONSTRAINT pk_number_sequences PRIMARY KEY (id),
    CONSTRAINT fk_number_sequences_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_number_sequences UNIQUE (tenant_id, module)
);

-- ---------------------------------------------------------------------------
-- Security: permissions, roles, users
-- ---------------------------------------------------------------------------
CREATE TABLE permissions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(100) NOT NULL,
    module      VARCHAR(50)  NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    code        VARCHAR(100) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_roles_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE users (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id            VARCHAR(36)  NOT NULL,
    username             VARCHAR(100) NOT NULL,
    email                VARCHAR(255) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    first_name           VARCHAR(100),
    last_name            VARCHAR(100),
    phone                VARCHAR(50),
    locale               VARCHAR(10),
    timezone             VARCHAR(100),
    avatar_path          VARCHAR(500),
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    is_tenant_admin      BOOLEAN      NOT NULL DEFAULT FALSE,
    manager_id           BIGINT,
    leave_validator_id   BIGINT,
    last_login_at        TIMESTAMP,
    password_changed_at  TIMESTAMP,
    failed_login_count   SMALLINT     NOT NULL DEFAULT 0,
    locked_until         TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at           TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users(id),
    CONSTRAINT fk_users_leave_validator FOREIGN KEY (leave_validator_id) REFERENCES users(id),
    CONSTRAINT uq_users_tenant_username UNIQUE (tenant_id, username),
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE user_roles (
    user_id    BIGINT    NOT NULL,
    role_id    BIGINT    NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_users_tenant       ON users(tenant_id);
CREATE INDEX idx_roles_tenant       ON roles(tenant_id);

-- ---------------------------------------------------------------------------
-- CRM: addresses, third parties, contacts, contact–third_party links
-- ---------------------------------------------------------------------------
CREATE TABLE addresses (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)  NOT NULL,
    label          VARCHAR(100),
    address_line1  VARCHAR(255) NOT NULL,
    address_line2  VARCHAR(255),
    city           VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code    VARCHAR(20),
    country_code   CHAR(2)      NOT NULL,
    is_billing     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_shipping    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_addresses PRIMARY KEY (id),
    CONSTRAINT fk_addresses_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE third_parties (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id           VARCHAR(36)  NOT NULL,
    code                VARCHAR(50)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    type                VARCHAR(30)  NOT NULL DEFAULT 'CUSTOMER',
    is_customer         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_supplier         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_prospect         BOOLEAN      NOT NULL DEFAULT FALSE,
    legal_form          VARCHAR(100),
    tax_id              VARCHAR(100),
    registration_no     VARCHAR(100),
    website             VARCHAR(255),
    phone               VARCHAR(50),
    fax                 VARCHAR(50),
    email               VARCHAR(255),
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    state_province      VARCHAR(100),
    postal_code         VARCHAR(20),
    country_code        CHAR(2),
    billing_address_id  BIGINT,
    shipping_address_id BIGINT,
    currency_code       CHAR(3),
    payment_terms       SMALLINT     DEFAULT 30,
    credit_limit        NUMERIC(19,4),
    balance             NUMERIC(19,4) NOT NULL DEFAULT 0,
    sales_rep_id        BIGINT,
    parent_id           BIGINT,
    notes               TEXT,
    tags                VARCHAR(500),
    status              VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    created_by          BIGINT,
    CONSTRAINT pk_third_parties PRIMARY KEY (id),
    CONSTRAINT fk_tp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tp_billing_addr FOREIGN KEY (billing_address_id) REFERENCES addresses(id),
    CONSTRAINT fk_tp_shipping_addr FOREIGN KEY (shipping_address_id) REFERENCES addresses(id),
    CONSTRAINT fk_tp_sales_rep FOREIGN KEY (sales_rep_id) REFERENCES users(id),
    CONSTRAINT fk_tp_parent FOREIGN KEY (parent_id) REFERENCES third_parties(id),
    CONSTRAINT uq_third_parties_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE contacts (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)  NOT NULL,
    civility       VARCHAR(10),
    first_name     VARCHAR(100),
    last_name      VARCHAR(100) NOT NULL,
    job_title      VARCHAR(150),
    department     VARCHAR(100),
    email          VARCHAR(255),
    email_secondary VARCHAR(255),
    phone          VARCHAR(50),
    mobile         VARCHAR(50),
    fax            VARCHAR(50),
    address_line1  VARCHAR(255),
    address_line2  VARCHAR(255),
    city           VARCHAR(100),
    state_province VARCHAR(100),
    postal_code    VARCHAR(20),
    country_code   CHAR(2),
    linkedin_url   VARCHAR(255),
    birth_date     DATE,
    notes          TEXT,
    is_primary     BOOLEAN      NOT NULL DEFAULT FALSE,
    opt_in_email   BOOLEAN      NOT NULL DEFAULT FALSE,
    status         VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    CONSTRAINT pk_contacts PRIMARY KEY (id),
    CONSTRAINT fk_contacts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE contact_third_party_links (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)  NOT NULL,
    contact_id      BIGINT       NOT NULL,
    third_party_id  BIGINT       NOT NULL,
    link_type_code  VARCHAR(50)  NOT NULL DEFAULT 'SALARIE',
    CONSTRAINT pk_contact_third_party_links PRIMARY KEY (id),
    CONSTRAINT fk_ctpl_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ctpl_contact FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE,
    CONSTRAINT fk_ctpl_third_party FOREIGN KEY (third_party_id) REFERENCES third_parties(id) ON DELETE CASCADE,
    CONSTRAINT uq_ctpl_contact_third_party UNIQUE (contact_id, third_party_id)
);

CREATE INDEX idx_third_parties_tenant ON third_parties(tenant_id);
CREATE INDEX idx_contacts_tenant      ON contacts(tenant_id);
CREATE INDEX idx_contact_third_party_links_contact ON contact_third_party_links(contact_id);
CREATE INDEX idx_contact_third_party_links_third_party ON contact_third_party_links(third_party_id);

-- ---------------------------------------------------------------------------
-- Catalog: currencies, products
-- ---------------------------------------------------------------------------
CREATE TABLE currencies (
    code           CHAR(3)     NOT NULL,
    name           VARCHAR(100) NOT NULL,
    symbol         VARCHAR(10)  NOT NULL,
    decimal_places SMALLINT     NOT NULL DEFAULT 2,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_currencies PRIMARY KEY (code)
);

CREATE TABLE exchange_rates (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id     VARCHAR(36)   NOT NULL,
    from_currency CHAR(3)       NOT NULL,
    to_currency   CHAR(3)       NOT NULL,
    rate          NUMERIC(19,8) NOT NULL,
    rate_date     DATE          NOT NULL,
    source        VARCHAR(50)   NOT NULL DEFAULT 'MANUAL',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_exchange_rates PRIMARY KEY (id),
    CONSTRAINT fk_er_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_er_from FOREIGN KEY (from_currency) REFERENCES currencies(code),
    CONSTRAINT fk_er_to   FOREIGN KEY (to_currency)   REFERENCES currencies(code),
    CONSTRAINT uq_exchange_rates UNIQUE (tenant_id, from_currency, to_currency, rate_date)
);

CREATE TABLE product_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    parent_id   BIGINT,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_product_categories PRIMARY KEY (id),
    CONSTRAINT fk_pc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_pc_parent FOREIGN KEY (parent_id) REFERENCES product_categories(id),
    CONSTRAINT uq_product_categories UNIQUE (tenant_id, code)
);

CREATE TABLE products (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id         VARCHAR(36)   NOT NULL,
    category_id       BIGINT,
    code              VARCHAR(100)  NOT NULL,
    name              VARCHAR(255)  NOT NULL,
    description       TEXT,
    type              VARCHAR(20)   NOT NULL DEFAULT 'PRODUCT',
    unit_of_measure   VARCHAR(50),
    sale_price        NUMERIC(19,4) NOT NULL DEFAULT 0,
    purchase_price    NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency_code     CHAR(3)       NOT NULL DEFAULT 'EUR',
    vat_rate          NUMERIC(6,4)  NOT NULL DEFAULT 0,
    is_sellable       BOOLEAN       NOT NULL DEFAULT TRUE,
    is_purchasable    BOOLEAN       NOT NULL DEFAULT TRUE,
    track_stock       BOOLEAN       NOT NULL DEFAULT FALSE,
    stock_quantity    NUMERIC(19,4) NOT NULL DEFAULT 0,
    stock_alert_level NUMERIC(19,4),
    barcode           VARCHAR(100),
    image_path        VARCHAR(500),
    notes             TEXT,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants(id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES product_categories(id),
    CONSTRAINT fk_products_currency FOREIGN KEY (currency_code) REFERENCES currencies(code),
    CONSTRAINT uq_products_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_products_tenant ON products(tenant_id);

-- ---------------------------------------------------------------------------
-- Commerce: proposals, orders, invoices, payments
-- ---------------------------------------------------------------------------
CREATE TABLE proposals (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id             VARCHAR(36)   NOT NULL,
    reference             VARCHAR(50)   NOT NULL,
    third_party_id        BIGINT        NOT NULL,
    contact_id            BIGINT,
    title                 VARCHAR(255),
    status                VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    date_issued           DATE          NOT NULL,
    date_valid_until      DATE,
    currency_code         CHAR(3)       NOT NULL,
    exchange_rate         NUMERIC(19,8) NOT NULL DEFAULT 1,
    subtotal              NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount       NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount            NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes                 TEXT,
    terms                 TEXT,
    sales_rep_id          BIGINT,
    converted_to_order_id BIGINT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT,
    CONSTRAINT pk_proposals PRIMARY KEY (id),
    CONSTRAINT fk_proposals_tenant      FOREIGN KEY (tenant_id)      REFERENCES tenants(id),
    CONSTRAINT fk_proposals_tp          FOREIGN KEY (third_party_id) REFERENCES third_parties(id),
    CONSTRAINT fk_proposals_contact     FOREIGN KEY (contact_id)     REFERENCES contacts(id),
    CONSTRAINT fk_proposals_currency    FOREIGN KEY (currency_code)  REFERENCES currencies(code),
    CONSTRAINT fk_proposals_sales_rep   FOREIGN KEY (sales_rep_id)   REFERENCES users(id),
    CONSTRAINT uq_proposals_ref         UNIQUE (tenant_id, reference)
);

CREATE TABLE proposal_lines (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(36)   NOT NULL,
    proposal_id  BIGINT        NOT NULL,
    product_id   BIGINT,
    sort_order   SMALLINT      NOT NULL DEFAULT 0,
    description  VARCHAR(500)  NOT NULL,
    quantity     NUMERIC(19,4) NOT NULL,
    unit_price   NUMERIC(19,4) NOT NULL,
    discount_pct NUMERIC(6,4)  NOT NULL DEFAULT 0,
    vat_rate     NUMERIC(6,4)  NOT NULL DEFAULT 0,
    line_total   NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3)      NOT NULL,
    CONSTRAINT pk_proposal_lines PRIMARY KEY (id),
    CONSTRAINT fk_pl_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants(id),
    CONSTRAINT fk_pl_proposal FOREIGN KEY (proposal_id) REFERENCES proposals(id),
    CONSTRAINT fk_pl_product  FOREIGN KEY (product_id)  REFERENCES products(id)
);

CREATE TABLE sales_orders (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id           VARCHAR(36)   NOT NULL,
    reference           VARCHAR(50)   NOT NULL,
    proposal_id         BIGINT,
    third_party_id      BIGINT        NOT NULL,
    contact_id          BIGINT,
    status              VARCHAR(30)   NOT NULL DEFAULT 'CONFIRMED',
    date_ordered        DATE          NOT NULL,
    date_expected       DATE,
    date_delivered      DATE,
    currency_code       CHAR(3)       NOT NULL,
    exchange_rate       NUMERIC(19,8) NOT NULL DEFAULT 1,
    subtotal            NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19,4) NOT NULL DEFAULT 0,
    shipping_address_id BIGINT,
    notes               TEXT,
    terms               TEXT,
    sales_rep_id        BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    CONSTRAINT pk_sales_orders PRIMARY KEY (id),
    CONSTRAINT fk_so_tenant   FOREIGN KEY (tenant_id)      REFERENCES tenants(id),
    CONSTRAINT fk_so_proposal FOREIGN KEY (proposal_id)    REFERENCES proposals(id),
    CONSTRAINT fk_so_tp       FOREIGN KEY (third_party_id) REFERENCES third_parties(id),
    CONSTRAINT fk_so_contact  FOREIGN KEY (contact_id)     REFERENCES contacts(id),
    CONSTRAINT fk_so_currency FOREIGN KEY (currency_code) REFERENCES currencies(code),
    CONSTRAINT uq_sales_orders_ref UNIQUE (tenant_id, reference)
);

CREATE TABLE sales_order_lines (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id          VARCHAR(36)   NOT NULL,
    order_id           BIGINT        NOT NULL,
    product_id         BIGINT,
    sort_order         SMALLINT      NOT NULL DEFAULT 0,
    description        VARCHAR(500)  NOT NULL,
    quantity           NUMERIC(19,4) NOT NULL,
    quantity_delivered NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit_price         NUMERIC(19,4) NOT NULL,
    discount_pct       NUMERIC(6,4)  NOT NULL DEFAULT 0,
    vat_rate           NUMERIC(6,4)  NOT NULL DEFAULT 0,
    line_total         NUMERIC(19,4) NOT NULL,
    CONSTRAINT pk_sales_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_sol_tenant  FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_sol_order   FOREIGN KEY (order_id)  REFERENCES sales_orders(id),
    CONSTRAINT fk_sol_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE purchase_orders (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)   NOT NULL,
    reference      VARCHAR(50)   NOT NULL,
    third_party_id BIGINT        NOT NULL,
    contact_id     BIGINT,
    status         VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    date_ordered   DATE          NOT NULL,
    date_expected  DATE,
    date_received  DATE,
    currency_code  CHAR(3)       NOT NULL,
    exchange_rate  NUMERIC(19,8) NOT NULL DEFAULT 1,
    subtotal       NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount   NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes          TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT,
    CONSTRAINT pk_purchase_orders PRIMARY KEY (id),
    CONSTRAINT fk_po_tenant   FOREIGN KEY (tenant_id)      REFERENCES tenants(id),
    CONSTRAINT fk_po_tp       FOREIGN KEY (third_party_id) REFERENCES third_parties(id),
    CONSTRAINT fk_po_contact  FOREIGN KEY (contact_id)     REFERENCES contacts(id),
    CONSTRAINT fk_po_currency FOREIGN KEY (currency_code)  REFERENCES currencies(code),
    CONSTRAINT uq_purchase_orders_ref UNIQUE (tenant_id, reference)
);

CREATE TABLE purchase_order_lines (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id         VARCHAR(36)   NOT NULL,
    order_id          BIGINT        NOT NULL,
    product_id        BIGINT,
    sort_order        SMALLINT      NOT NULL DEFAULT 0,
    description       VARCHAR(500)  NOT NULL,
    quantity          NUMERIC(19,4) NOT NULL,
    quantity_received NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit_price        NUMERIC(19,4) NOT NULL,
    vat_rate          NUMERIC(6,4)  NOT NULL DEFAULT 0,
    line_total        NUMERIC(19,4) NOT NULL,
    CONSTRAINT pk_purchase_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_pol_tenant  FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_pol_order   FOREIGN KEY (order_id)  REFERENCES purchase_orders(id),
    CONSTRAINT fk_pol_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE invoices (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id         VARCHAR(36)   NOT NULL,
    reference         VARCHAR(50)   NOT NULL,
    type              VARCHAR(30)   NOT NULL,
    third_party_id    BIGINT        NOT NULL,
    contact_id        BIGINT,
    sales_order_id    BIGINT,
    purchase_order_id BIGINT,
    status            VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    date_issued       DATE          NOT NULL,
    date_due          DATE          NOT NULL,
    currency_code     CHAR(3)       NOT NULL,
    exchange_rate     NUMERIC(19,8) NOT NULL DEFAULT 1,
    subtotal          NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount   NUMERIC(19,4) NOT NULL DEFAULT 0,
    vat_amount        NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount      NUMERIC(19,4) NOT NULL DEFAULT 0,
    amount_paid       NUMERIC(19,4) NOT NULL DEFAULT 0,
    amount_remaining  NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes             TEXT,
    terms             TEXT,
    payment_method    VARCHAR(50),
    bank_account      VARCHAR(100),
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validated_at      TIMESTAMP,
    validated_by      BIGINT,
    created_by        BIGINT,
    CONSTRAINT pk_invoices PRIMARY KEY (id),
    CONSTRAINT fk_inv_tenant    FOREIGN KEY (tenant_id)        REFERENCES tenants(id),
    CONSTRAINT fk_inv_tp        FOREIGN KEY (third_party_id)   REFERENCES third_parties(id),
    CONSTRAINT fk_inv_contact   FOREIGN KEY (contact_id)       REFERENCES contacts(id),
    CONSTRAINT fk_inv_so        FOREIGN KEY (sales_order_id)   REFERENCES sales_orders(id),
    CONSTRAINT fk_inv_po        FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id),
    CONSTRAINT fk_inv_currency  FOREIGN KEY (currency_code)    REFERENCES currencies(code),
    CONSTRAINT uq_invoices_ref  UNIQUE (tenant_id, reference)
);

CREATE TABLE invoice_lines (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(36)   NOT NULL,
    invoice_id   BIGINT        NOT NULL,
    product_id   BIGINT,
    sort_order   SMALLINT      NOT NULL DEFAULT 0,
    description  VARCHAR(500)  NOT NULL,
    quantity     NUMERIC(19,4) NOT NULL,
    unit_price   NUMERIC(19,4) NOT NULL,
    discount_pct NUMERIC(6,4)  NOT NULL DEFAULT 0,
    vat_rate     NUMERIC(6,4)  NOT NULL DEFAULT 0,
    line_total   NUMERIC(19,4) NOT NULL,
    CONSTRAINT pk_invoice_lines PRIMARY KEY (id),
    CONSTRAINT fk_il_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants(id),
    CONSTRAINT fk_il_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_il_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE payments (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)   NOT NULL,
    invoice_id     BIGINT        NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    currency_code  CHAR(3)       NOT NULL,
    exchange_rate  NUMERIC(19,8) NOT NULL DEFAULT 1,
    payment_date   DATE          NOT NULL,
    payment_method VARCHAR(50)   NOT NULL,
    reference      VARCHAR(100),
    notes          TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_pay_tenant   FOREIGN KEY (tenant_id)   REFERENCES tenants(id),
    CONSTRAINT fk_pay_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices(id),
    CONSTRAINT fk_pay_currency FOREIGN KEY (currency_code) REFERENCES currencies(code)
);

CREATE INDEX idx_proposals_tenant    ON proposals(tenant_id);
CREATE INDEX idx_sales_orders_tenant ON sales_orders(tenant_id);
CREATE INDEX idx_invoices_tenant     ON invoices(tenant_id);
CREATE INDEX idx_invoices_type       ON invoices(tenant_id, type, status);
CREATE INDEX idx_invoices_tp         ON invoices(tenant_id, third_party_id);

-- ---------------------------------------------------------------------------
-- Project & Agenda
-- ---------------------------------------------------------------------------
CREATE TABLE projects (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(36)   NOT NULL,
    code           VARCHAR(50)   NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    description    TEXT,
    third_party_id BIGINT,
    status         VARCHAR(30)   NOT NULL DEFAULT 'PLANNING',
    priority       VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    date_start     DATE,
    date_end       DATE,
    budget_amount  NUMERIC(19,4),
    currency_code  CHAR(3),
    manager_id     BIGINT,
    billing_mode   VARCHAR(30)   DEFAULT 'FIXED',
    notes          TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_proj_tenant   FOREIGN KEY (tenant_id)      REFERENCES tenants(id),
    CONSTRAINT fk_proj_tp       FOREIGN KEY (third_party_id) REFERENCES third_parties(id),
    CONSTRAINT fk_proj_currency FOREIGN KEY (currency_code)  REFERENCES currencies(code),
    CONSTRAINT fk_proj_manager  FOREIGN KEY (manager_id)     REFERENCES users(id),
    CONSTRAINT uq_projects_code UNIQUE (tenant_id, code)
);

CREATE TABLE project_members (
    project_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_members PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES users(id)
);

CREATE TABLE project_tasks (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)  NOT NULL,
    project_id      BIGINT      NOT NULL,
    parent_task_id  BIGINT,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(30)  NOT NULL DEFAULT 'TODO',
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    assigned_to_id  BIGINT,
    date_start      DATE,
    date_due        DATE,
    date_completed  DATE,
    estimated_hours NUMERIC(8,2),
    logged_hours    NUMERIC(8,2) NOT NULL DEFAULT 0,
    progress_pct    SMALLINT     NOT NULL DEFAULT 0,
    sort_order      SMALLINT     NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_tasks PRIMARY KEY (id),
    CONSTRAINT fk_pt_tenant      FOREIGN KEY (tenant_id)      REFERENCES tenants(id),
    CONSTRAINT fk_pt_project     FOREIGN KEY (project_id)     REFERENCES projects(id),
    CONSTRAINT fk_pt_parent      FOREIGN KEY (parent_task_id) REFERENCES project_tasks(id),
    CONSTRAINT fk_pt_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES users(id)
);

CREATE TABLE time_entries (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)  NOT NULL,
    task_id         BIGINT,
    project_id      BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    date_worked     DATE         NOT NULL,
    hours           NUMERIC(8,2) NOT NULL,
    description     TEXT,
    is_billable     BOOLEAN      NOT NULL DEFAULT TRUE,
    invoice_line_id BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_time_entries PRIMARY KEY (id),
    CONSTRAINT fk_te_tenant  FOREIGN KEY (tenant_id)  REFERENCES tenants(id),
    CONSTRAINT fk_te_task    FOREIGN KEY (task_id)    REFERENCES project_tasks(id),
    CONSTRAINT fk_te_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_te_user    FOREIGN KEY (user_id)    REFERENCES users(id)
);

CREATE TABLE agenda_events (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       VARCHAR(36)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(30)  NOT NULL DEFAULT 'MEETING',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    all_day         BOOLEAN      NOT NULL DEFAULT FALSE,
    start_datetime  TIMESTAMP    NOT NULL,
    end_datetime    TIMESTAMP,
    location        VARCHAR(500),
    organizer_id    BIGINT       NOT NULL,
    third_party_id  BIGINT,
    contact_id      BIGINT,
    project_id      BIGINT,
    recurrence_rule VARCHAR(500),
    recurrence_end  DATE,
    parent_event_id BIGINT,
    color           VARCHAR(7),
    is_private      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_agenda_events PRIMARY KEY (id),
    CONSTRAINT fk_ae_tenant      FOREIGN KEY (tenant_id)     REFERENCES tenants(id),
    CONSTRAINT fk_ae_organizer   FOREIGN KEY (organizer_id)  REFERENCES users(id),
    CONSTRAINT fk_ae_tp          FOREIGN KEY (third_party_id) REFERENCES third_parties(id),
    CONSTRAINT fk_ae_contact     FOREIGN KEY (contact_id)    REFERENCES contacts(id),
    CONSTRAINT fk_ae_project     FOREIGN KEY (project_id)    REFERENCES projects(id),
    CONSTRAINT fk_ae_parent     FOREIGN KEY (parent_event_id) REFERENCES agenda_events(id)
);

CREATE TABLE agenda_event_attendees (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    event_id   BIGINT      NOT NULL,
    user_id    BIGINT,
    contact_id BIGINT,
    status     VARCHAR(20) NOT NULL DEFAULT 'INVITED',
    notified_at TIMESTAMP,
    CONSTRAINT pk_agenda_event_attendees PRIMARY KEY (id),
    CONSTRAINT fk_aea_event   FOREIGN KEY (event_id)   REFERENCES agenda_events(id),
    CONSTRAINT fk_aea_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_aea_contact FOREIGN KEY (contact_id) REFERENCES contacts(id)
);

CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    user_id     BIGINT,
    username    VARCHAR(100),
    ip_address  VARCHAR(45),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   VARCHAR(100),
    old_values  TEXT,
    new_values  TEXT,
    occurred_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_projects_tenant     ON projects(tenant_id);
CREATE INDEX idx_project_tasks_tenant ON project_tasks(tenant_id, project_id);
CREATE INDEX idx_agenda_events_range  ON agenda_events(tenant_id, start_datetime, end_datetime);
CREATE INDEX idx_audit_log_entity     ON audit_log(tenant_id, entity_type, entity_id);

-- ---------------------------------------------------------------------------
-- Extrafields & field visibility
-- ---------------------------------------------------------------------------
CREATE TABLE extrafield_definitions (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        VARCHAR(36)  NOT NULL,
    entity_type      VARCHAR(50)  NOT NULL,
    field_code       VARCHAR(50)  NOT NULL,
    label            VARCHAR(255) NOT NULL,
    field_type       VARCHAR(30)  NOT NULL,
    field_options    TEXT,
    default_value    VARCHAR(500),
    is_required      BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order       SMALLINT     NOT NULL DEFAULT 0,
    visible_on_list  BOOLEAN      NOT NULL DEFAULT FALSE,
    visible_on_detail BOOLEAN     NOT NULL DEFAULT TRUE,
    visible_on_form  BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_extrafield_definitions PRIMARY KEY (id),
    CONSTRAINT fk_efd_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_efd_tenant_entity_code UNIQUE (tenant_id, entity_type, field_code)
);

CREATE INDEX idx_efd_tenant_entity ON extrafield_definitions(tenant_id, entity_type);

CREATE TABLE extrafield_values (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    field_code  VARCHAR(50)  NOT NULL,
    value_text  TEXT,
    CONSTRAINT pk_extrafield_values PRIMARY KEY (id),
    CONSTRAINT fk_efv_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_efv_entity_field UNIQUE (tenant_id, entity_type, entity_id, field_code)
);

CREATE INDEX idx_efv_entity ON extrafield_values(tenant_id, entity_type, entity_id);

CREATE TABLE field_visibility_config (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id         VARCHAR(36)  NOT NULL,
    entity_type       VARCHAR(50)  NOT NULL,
    field_name        VARCHAR(100) NOT NULL,
    visible_on_list   BOOLEAN      NOT NULL DEFAULT TRUE,
    visible_on_detail BOOLEAN      NOT NULL DEFAULT TRUE,
    visible_on_form   BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order        SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_field_visibility_config PRIMARY KEY (id),
    CONSTRAINT fk_fvc_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_fvc_tenant_entity_field UNIQUE (tenant_id, entity_type, field_name)
);

CREATE INDEX idx_fvc_tenant_entity ON field_visibility_config(tenant_id, entity_type);

-- ---------------------------------------------------------------------------
-- Email templates
-- ---------------------------------------------------------------------------
CREATE TABLE email_templates (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    template_key VARCHAR(50)  NOT NULL,
    subject     VARCHAR(500) NOT NULL,
    body_html   TEXT,
    body_text   TEXT,
    locale      VARCHAR(10)  NOT NULL DEFAULT 'fr',
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_email_templates PRIMARY KEY (id),
    CONSTRAINT uq_email_templates_key_locale UNIQUE (template_key, locale)
);

-- ---------------------------------------------------------------------------
-- Dictionary items
-- ---------------------------------------------------------------------------
CREATE TABLE dictionary_items (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_dictionary_items PRIMARY KEY (id),
    CONSTRAINT fk_dictionary_items_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_dictionary_items UNIQUE (tenant_id, type, code)
);

CREATE INDEX idx_dictionary_items_tenant_type ON dictionary_items(tenant_id, type);

-- ---------------------------------------------------------------------------
-- No-code: module definitions, custom entities
-- ---------------------------------------------------------------------------
CREATE TABLE module_definitions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    code                VARCHAR(80)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(1000),
    version             VARCHAR(50)  NOT NULL DEFAULT '1.0.0',
    author_tenant_id    VARCHAR(36),
    is_public           BOOLEAN      NOT NULL DEFAULT FALSE,
    schema_json         TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_module_definitions PRIMARY KEY (id),
    CONSTRAINT uq_module_definitions_code_version UNIQUE (code, version)
);

CREATE TABLE custom_entity_definitions (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    module_definition_id  BIGINT       NOT NULL,
    code                  VARCHAR(80)  NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    description           VARCHAR(500),
    fields_schema         TEXT,
    list_columns          TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_custom_entity_definitions PRIMARY KEY (id),
    CONSTRAINT fk_ced_module FOREIGN KEY (module_definition_id) REFERENCES module_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uq_ced_module_code UNIQUE (module_definition_id, code)
);

CREATE TABLE custom_entity_data (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id               VARCHAR(36)  NOT NULL,
    entity_definition_id    BIGINT       NOT NULL,
    payload                 TEXT,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_custom_entity_data PRIMARY KEY (id),
    CONSTRAINT fk_cedata_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_cedata_definition FOREIGN KEY (entity_definition_id) REFERENCES custom_entity_definitions(id) ON DELETE CASCADE
);

CREATE INDEX idx_custom_entity_data_tenant ON custom_entity_data(tenant_id);
CREATE INDEX idx_custom_entity_data_definition ON custom_entity_data(entity_definition_id);
CREATE INDEX idx_module_definitions_public ON module_definitions(is_public);

-- ---------------------------------------------------------------------------
-- Tenant modules (with optional module_definition_id)
-- ---------------------------------------------------------------------------
CREATE TABLE tenant_modules (
    tenant_id             VARCHAR(36)  NOT NULL,
    module_code           VARCHAR(80)  NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    settings              TEXT,
    module_definition_id  BIGINT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tenant_modules PRIMARY KEY (tenant_id, module_code),
    CONSTRAINT fk_tenant_modules_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tenant_modules_definition FOREIGN KEY (module_definition_id) REFERENCES module_definitions(id) ON DELETE SET NULL
);

CREATE INDEX idx_tenant_modules_tenant ON tenant_modules(tenant_id);
CREATE INDEX idx_tenant_modules_definition ON tenant_modules(module_definition_id);

-- ---------------------------------------------------------------------------
-- Leave module
-- ---------------------------------------------------------------------------
CREATE TABLE leave_types (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   VARCHAR(36)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(255) NOT NULL,
    color       VARCHAR(7),
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_leave_types PRIMARY KEY (id),
    CONSTRAINT fk_leave_types_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uq_leave_types_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE leave_requests (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        VARCHAR(36)  NOT NULL,
    requester_id     BIGINT       NOT NULL,
    leave_type_id    BIGINT       NOT NULL,
    date_start       DATE         NOT NULL,
    date_end         DATE         NOT NULL,
    half_day_start   BOOLEAN      NOT NULL DEFAULT FALSE,
    half_day_end     BOOLEAN      NOT NULL DEFAULT FALSE,
    status           VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    comment          TEXT,
    submitted_at     TIMESTAMP,
    validator_id     BIGINT,
    approved_by_id   BIGINT,
    approved_at      TIMESTAMP,
    denied_by_id     BIGINT,
    denied_at        TIMESTAMP,
    response_comment TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_leave_requests PRIMARY KEY (id),
    CONSTRAINT fk_lr_tenant       FOREIGN KEY (tenant_id)      REFERENCES tenants(id),
    CONSTRAINT fk_lr_requester    FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_lr_leave_type   FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_lr_validator    FOREIGN KEY (validator_id)  REFERENCES users(id),
    CONSTRAINT fk_lr_approved_by  FOREIGN KEY (approved_by_id) REFERENCES users(id),
    CONSTRAINT fk_lr_denied_by    FOREIGN KEY (denied_by_id)  REFERENCES users(id)
);

CREATE INDEX idx_leave_types_tenant ON leave_types(tenant_id);
CREATE INDEX idx_leave_requests_tenant_status ON leave_requests(tenant_id, status);
CREATE INDEX idx_leave_requests_tenant_requester ON leave_requests(tenant_id, requester_id);
CREATE INDEX idx_leave_requests_dates ON leave_requests(tenant_id, date_start, date_end);

-- ============================================================
-- SEEDS
-- ============================================================

-- Tenant
INSERT INTO tenants (id, code, name, default_locale, default_currency, timezone, plan)
VALUES ('00000000-0000-0000-0000-000000000001', 'aplose', 'Aplose Demo', 'en', 'EUR', 'Europe/Paris', 'enterprise');

-- Permissions (all from V2 + V7 + V12 + V16 + V17)
INSERT INTO permissions (code, module, action, description) VALUES
  ('USER_CREATE', 'USER', 'CREATE', 'Create users'),
  ('USER_READ', 'USER', 'READ', 'View users'),
  ('USER_UPDATE', 'USER', 'UPDATE', 'Edit users'),
  ('USER_DELETE', 'USER', 'DELETE', 'Delete users'),
  ('USER_ADMIN', 'USER', 'ADMIN', 'Full user management'),
  ('ROLE_CREATE', 'ROLE', 'CREATE', 'Create roles'),
  ('ROLE_READ', 'ROLE', 'READ', 'View roles'),
  ('ROLE_UPDATE', 'ROLE', 'UPDATE', 'Edit roles'),
  ('ROLE_DELETE', 'ROLE', 'DELETE', 'Delete roles'),
  ('ROLE_ADMIN', 'ROLE', 'ADMIN', 'Full role management'),
  ('TENANT_READ', 'TENANT', 'READ', 'View tenant settings'),
  ('TENANT_UPDATE', 'TENANT', 'UPDATE', 'Edit tenant settings'),
  ('TENANT_ADMIN', 'TENANT', 'ADMIN', 'Full tenant administration'),
  ('THIRD_PARTY_CREATE', 'THIRD_PARTY', 'CREATE', 'Create third parties'),
  ('THIRD_PARTY_READ', 'THIRD_PARTY', 'READ', 'View third parties'),
  ('THIRD_PARTY_UPDATE', 'THIRD_PARTY', 'UPDATE', 'Edit third parties'),
  ('THIRD_PARTY_DELETE', 'THIRD_PARTY', 'DELETE', 'Delete third parties'),
  ('CONTACT_CREATE', 'CONTACT', 'CREATE', 'Create contacts'),
  ('CONTACT_READ', 'CONTACT', 'READ', 'View contacts'),
  ('CONTACT_UPDATE', 'CONTACT', 'UPDATE', 'Edit contacts'),
  ('CONTACT_DELETE', 'CONTACT', 'DELETE', 'Delete contacts'),
  ('PRODUCT_CREATE', 'PRODUCT', 'CREATE', 'Create products'),
  ('PRODUCT_READ', 'PRODUCT', 'READ', 'View products'),
  ('PRODUCT_UPDATE', 'PRODUCT', 'UPDATE', 'Edit products'),
  ('PRODUCT_DELETE', 'PRODUCT', 'DELETE', 'Delete products'),
  ('PROPOSAL_CREATE', 'PROPOSAL', 'CREATE', 'Create proposals'),
  ('PROPOSAL_READ', 'PROPOSAL', 'READ', 'View proposals'),
  ('PROPOSAL_UPDATE', 'PROPOSAL', 'UPDATE', 'Edit proposals'),
  ('PROPOSAL_DELETE', 'PROPOSAL', 'DELETE', 'Delete proposals'),
  ('PROPOSAL_APPROVE', 'PROPOSAL', 'APPROVE', 'Approve/send proposals'),
  ('SALES_ORDER_CREATE', 'SALES_ORDER', 'CREATE', 'Create sales orders'),
  ('SALES_ORDER_READ', 'SALES_ORDER', 'READ', 'View sales orders'),
  ('SALES_ORDER_UPDATE', 'SALES_ORDER', 'UPDATE', 'Edit sales orders'),
  ('SALES_ORDER_DELETE', 'SALES_ORDER', 'DELETE', 'Delete sales orders'),
  ('PURCHASE_ORDER_CREATE', 'PURCHASE_ORDER', 'CREATE', 'Create purchase orders'),
  ('PURCHASE_ORDER_READ', 'PURCHASE_ORDER', 'READ', 'View purchase orders'),
  ('PURCHASE_ORDER_UPDATE', 'PURCHASE_ORDER', 'UPDATE', 'Edit purchase orders'),
  ('PURCHASE_ORDER_DELETE', 'PURCHASE_ORDER', 'DELETE', 'Delete purchase orders'),
  ('INVOICE_CREATE', 'INVOICE', 'CREATE', 'Create invoices'),
  ('INVOICE_READ', 'INVOICE', 'READ', 'View invoices'),
  ('INVOICE_UPDATE', 'INVOICE', 'UPDATE', 'Edit invoices'),
  ('INVOICE_DELETE', 'INVOICE', 'DELETE', 'Delete invoices'),
  ('INVOICE_VALIDATE', 'INVOICE', 'VALIDATE', 'Validate/approve invoices'),
  ('PAYMENT_CREATE', 'PAYMENT', 'CREATE', 'Record payments'),
  ('PAYMENT_READ', 'PAYMENT', 'READ', 'View payments'),
  ('PAYMENT_DELETE', 'PAYMENT', 'DELETE', 'Delete payments'),
  ('CURRENCY_ADMIN', 'CURRENCY', 'ADMIN', 'Manage currencies and rates'),
  ('PROJECT_CREATE', 'PROJECT', 'CREATE', 'Create projects'),
  ('PROJECT_READ', 'PROJECT', 'READ', 'View projects'),
  ('PROJECT_UPDATE', 'PROJECT', 'UPDATE', 'Edit projects'),
  ('PROJECT_DELETE', 'PROJECT', 'DELETE', 'Delete projects'),
  ('TASK_CREATE', 'TASK', 'CREATE', 'Create tasks'),
  ('TASK_READ', 'TASK', 'READ', 'View tasks'),
  ('TASK_UPDATE', 'TASK', 'UPDATE', 'Edit tasks'),
  ('TASK_DELETE', 'TASK', 'DELETE', 'Delete tasks'),
  ('TIME_ENTRY_CREATE', 'TIME_ENTRY', 'CREATE', 'Log time entries'),
  ('TIME_ENTRY_READ', 'TIME_ENTRY', 'READ', 'View time entries'),
  ('TIME_ENTRY_UPDATE', 'TIME_ENTRY', 'UPDATE', 'Edit time entries'),
  ('TIME_ENTRY_DELETE', 'TIME_ENTRY', 'DELETE', 'Delete time entries'),
  ('AGENDA_CREATE', 'AGENDA', 'CREATE', 'Create events'),
  ('AGENDA_READ', 'AGENDA', 'READ', 'View events'),
  ('AGENDA_UPDATE', 'AGENDA', 'UPDATE', 'Edit events'),
  ('AGENDA_DELETE', 'AGENDA', 'DELETE', 'Delete events'),
  ('REPORT_READ', 'REPORT', 'READ', 'View reports'),
  ('REPORT_EXPORT', 'REPORT', 'EXPORT', 'Export reports'),
  ('EXTRAFIELD_ADMIN', 'EXTRAFIELD', 'ADMIN', 'Manage custom extra fields'),
  ('FIELD_CONFIG_ADMIN', 'FIELD_CONFIG', 'ADMIN', 'Manage field visibility configuration'),
  ('CUSTOM_ENTITY_CREATE', 'CUSTOM_ENTITY', 'CREATE', 'Create custom entity records'),
  ('CUSTOM_ENTITY_READ', 'CUSTOM_ENTITY', 'READ', 'View custom entity records'),
  ('CUSTOM_ENTITY_UPDATE', 'CUSTOM_ENTITY', 'UPDATE', 'Edit custom entity records'),
  ('CUSTOM_ENTITY_DELETE', 'CUSTOM_ENTITY', 'DELETE', 'Delete custom entity records'),
  ('BANK_READ', 'BANK', 'READ', 'View bank'),
  ('ACCOUNTING_READ', 'ACCOUNTING', 'READ', 'View accounting'),
  ('HR_READ', 'HR', 'READ', 'View HR'),
  ('GED_READ', 'GED', 'READ', 'View document management'),
  ('TICKETING_READ', 'TICKETING', 'READ', 'View ticketing'),
  ('LEAVE_READ', 'LEAVE', 'READ', 'View leave requests'),
  ('LEAVE_CREATE', 'LEAVE', 'CREATE', 'Create leave requests'),
  ('LEAVE_UPDATE', 'LEAVE', 'UPDATE', 'Edit leave requests'),
  ('LEAVE_DELETE', 'LEAVE', 'DELETE', 'Delete leave requests'),
  ('LEAVE_APPROVE', 'LEAVE', 'APPROVE', 'Approve or deny leave requests');

-- Role SUPER_ADMIN
INSERT INTO roles (tenant_id, code, name, description, is_system)
VALUES ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super Administrator', 'Full access to all modules', TRUE);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001';

-- Admin user (password: Admin1234!)
INSERT INTO users (tenant_id, username, email, password_hash, first_name, last_name, is_active, is_tenant_admin)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@aplose.fr',
    '$2a$12$OdQb9rvBWvIGlJFD5.YwmOCWmhh4CX/g9X5k5vH4J3z5z4R5z4R5.',
    'Admin',
    'Aplose',
    TRUE,
    TRUE
);

INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT u.id, r.id, u.id
FROM users u, roles r
WHERE u.username = 'admin'
  AND u.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND r.code = 'SUPER_ADMIN'
  AND r.tenant_id = '00000000-0000-0000-0000-000000000001';

-- Currencies
INSERT INTO currencies (code, name, symbol, decimal_places) VALUES
  ('EUR', 'Euro', '€', 2),
  ('USD', 'US Dollar', '$', 2),
  ('GBP', 'British Pound', '£', 2),
  ('CHF', 'Swiss Franc', 'Fr', 2),
  ('JPY', 'Japanese Yen', '¥', 0),
  ('CAD', 'Canadian Dollar', 'C$', 2),
  ('AUD', 'Australian Dollar', 'A$', 2),
  ('MAD', 'Moroccan Dirham', 'د.م.', 2),
  ('TND', 'Tunisian Dinar', 'د.ت', 3),
  ('DZD', 'Algerian Dinar', 'د.ج', 2),
  ('XOF', 'West African CFA Franc', 'CFA', 0),
  ('XAF', 'Central African CFA Franc', 'FCFA', 0);

-- Email templates (welcome, onboarding, leave)
INSERT INTO email_templates (template_key, subject, body_html, body_text, locale) VALUES
('welcome',
 'Bienvenue sur AploseERP',
 '<p>Bonjour {{userName}},</p><p>Votre entreprise <strong>{{companyName}}</strong> a été créée sur AploseERP.</p><p>Connectez-vous ici : <a href="{{loginUrl}}">{{loginUrl}}</a></p><p>À bientôt,<br/>L''équipe AploseERP</p>',
 'Bonjour {{userName}}, Votre entreprise {{companyName}} a été créée sur AploseERP. Connectez-vous ici : {{loginUrl}} À bientôt, L''équipe AploseERP',
 'fr'),
('onboarding',
 'Découvrez AploseERP - Quelques pistes pour démarrer',
 '<p>Bonjour {{userName}},</p><p>Voici quelques étapes pour bien démarrer avec AploseERP :</p><ul><li>Complétez les paramètres de votre entreprise (Paramètres)</li><li>Ajoutez vos premiers tiers et contacts (CRM)</li><li>Créez un devis ou une facture (Commerce)</li></ul><p><a href="{{loginUrl}}">Accéder à mon espace</a></p><p>L''équipe AploseERP</p>',
 'Bonjour {{userName}}, Découvrez AploseERP : complétez vos paramètres, ajoutez vos tiers et créez vos premiers devis. Accéder à mon espace : {{loginUrl}}',
 'fr'),
('leave.request.submitted',
 'Demande de congés à valider - {{requesterName}}',
 '<p>Bonjour {{validatorName}},</p><p><strong>{{requesterName}}</strong> a soumis une demande de congés.</p><p>Type : {{leaveTypeLabel}}, du {{dateStart}} au {{dateEnd}}.</p><p><a href="{{requestUrl}}">Voir la demande et valider</a></p><p>L''équipe AploseERP</p>',
 'Bonjour {{validatorName}}, {{requesterName}} a soumis une demande de congés ({{leaveTypeLabel}}, {{dateStart}} - {{dateEnd}}). Voir : {{requestUrl}}',
 'fr'),
('leave.request.denied',
 'Votre demande de congés a été refusée',
 '<p>Bonjour {{requesterName}},</p><p>Votre demande de congés a été refusée.</p><p>Commentaire du validateur : {{responseComment}}</p><p><a href="{{requestUrl}}">Voir la demande</a></p><p>L''équipe AploseERP</p>',
 'Bonjour {{requesterName}}, votre demande de congés a été refusée. Commentaire : {{responseComment}}. Voir : {{requestUrl}}',
 'fr');

-- Dictionary items (CIVILITY, COUNTRY, CURRENCY, LEGAL_FORM, PAYMENT_METHOD, CONTACT_THIRD_PARTY_LINK_TYPE)
INSERT INTO dictionary_items (tenant_id, type, code, label, sort_order) VALUES
  ('00000000-0000-0000-0000-000000000001', 'CIVILITY', 'M', 'M.', 1),
  ('00000000-0000-0000-0000-000000000001', 'CIVILITY', 'MME', 'Mme', 2),
  ('00000000-0000-0000-0000-000000000001', 'CIVILITY', 'MLLE', 'Mlle', 3),
  ('00000000-0000-0000-0000-000000000001', 'CIVILITY', 'DR', 'Dr.', 4),
  ('00000000-0000-0000-0000-000000000001', 'CIVILITY', 'MR', 'Mr', 5),
  ('00000000-0000-0000-0000-000000000001', 'CIVILITY', 'MRS', 'Mrs', 6),
  ('00000000-0000-0000-0000-000000000001', 'CIVILITY', 'MS', 'Ms', 7),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'FR', 'France', 1),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'BE', 'Belgique', 2),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'CH', 'Suisse', 3),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'LU', 'Luxembourg', 4),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'DE', 'Allemagne', 5),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'ES', 'Espagne', 6),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'IT', 'Italie', 7),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'GB', 'Royaume-Uni', 8),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'US', 'États-Unis', 9),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'MA', 'Maroc', 10),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'TN', 'Tunisie', 11),
  ('00000000-0000-0000-0000-000000000001', 'COUNTRY', 'DZ', 'Algérie', 12),
  ('00000000-0000-0000-0000-000000000001', 'CURRENCY', 'EUR', 'Euro', 1),
  ('00000000-0000-0000-0000-000000000001', 'CURRENCY', 'USD', 'US Dollar', 2),
  ('00000000-0000-0000-0000-000000000001', 'CURRENCY', 'GBP', 'British Pound', 3),
  ('00000000-0000-0000-0000-000000000001', 'CURRENCY', 'CHF', 'Swiss Franc', 4),
  ('00000000-0000-0000-0000-000000000001', 'CURRENCY', 'MAD', 'Moroccan Dirham', 5),
  ('00000000-0000-0000-0000-000000000001', 'CURRENCY', 'TND', 'Tunisian Dinar', 6),
  ('00000000-0000-0000-0000-000000000001', 'CURRENCY', 'DZD', 'Algerian Dinar', 7),
  ('00000000-0000-0000-0000-000000000001', 'LEGAL_FORM', 'SARL', 'SARL', 1),
  ('00000000-0000-0000-0000-000000000001', 'LEGAL_FORM', 'SAS', 'SAS', 2),
  ('00000000-0000-0000-0000-000000000001', 'LEGAL_FORM', 'SASU', 'SASU', 3),
  ('00000000-0000-0000-0000-000000000001', 'LEGAL_FORM', 'SA', 'SA', 4),
  ('00000000-0000-0000-0000-000000000001', 'LEGAL_FORM', 'EURL', 'EURL', 5),
  ('00000000-0000-0000-0000-000000000001', 'LEGAL_FORM', 'EI', 'Entreprise individuelle', 6),
  ('00000000-0000-0000-0000-000000000001', 'LEGAL_FORM', 'AUTO', 'Auto-entrepreneur', 7),
  ('00000000-0000-0000-0000-000000000001', 'PAYMENT_METHOD', 'BANK', 'Virement', 1),
  ('00000000-0000-0000-0000-000000000001', 'PAYMENT_METHOD', 'CHECK', 'Chèque', 2),
  ('00000000-0000-0000-0000-000000000001', 'PAYMENT_METHOD', 'CARD', 'Carte bancaire', 3),
  ('00000000-0000-0000-0000-000000000001', 'PAYMENT_METHOD', 'CASH', 'Espèces', 4),
  ('00000000-0000-0000-0000-000000000001', 'CONTACT_THIRD_PARTY_LINK_TYPE', 'SALARIE', 'Salarié', 1),
  ('00000000-0000-0000-0000-000000000001', 'CONTACT_THIRD_PARTY_LINK_TYPE', 'ACTIONNAIRE', 'Actionnaire', 2),
  ('00000000-0000-0000-0000-000000000001', 'CONTACT_THIRD_PARTY_LINK_TYPE', 'PRESTATAIRE_EXTERNE', 'Prestataire externe', 3);

-- Tenant modules (no CATALOG_CURRENCY)
INSERT INTO tenant_modules (tenant_id, module_code, enabled)
VALUES ('00000000-0000-0000-0000-000000000001', 'CRM_THIRD_PARTY', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'CRM_CONTACT', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'COMMERCE_PROPOSAL', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'COMMERCE_ORDER', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'COMMERCE_INVOICE', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'CATALOG_PRODUCT', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'PROJECT', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'AGENDA', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'BANK', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'ACCOUNTING', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'HR', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'GED', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'TICKETING', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'REPORTING', TRUE),
       ('00000000-0000-0000-0000-000000000001', 'LEAVE_REQUEST', TRUE);

-- Sample no-code module (farm)
INSERT INTO module_definitions (code, name, description, version, author_tenant_id, is_public, schema_json)
VALUES (
    'farm',
    'Gestion de ferme',
    'Module exemple : suivi des animaux (no-code).',
    '1.0.0',
    NULL,
    TRUE,
    '{"code":"farm","name":"Gestion de ferme","version":"1.0.0","businessObjects":[{"code":"ANIMAL","label":"Animal","icon":"bi-egg","fields":[{"name":"name","type":"string","label":"Nom","required":true},{"name":"species","type":"string","label":"Espèce"},{"name":"birthDate","type":"date","label":"Date de naissance"}],"listColumns":["name","species","birthDate"]}]}'
);

INSERT INTO custom_entity_definitions (module_definition_id, code, name, description, fields_schema, list_columns)
VALUES (
    1,
    'ANIMAL',
    'Animal',
    'Fiche animal',
    '[{"name":"name","type":"string","label":"Nom","required":true},{"name":"species","type":"string","label":"Espèce"},{"name":"birthDate","type":"date","label":"Date de naissance"}]',
    '["name","species","birthDate"]'
);

-- Leave types (demo tenant)
INSERT INTO leave_types (tenant_id, code, label, color, sort_order) VALUES
  ('00000000-0000-0000-0000-000000000001', 'PAID_LEAVE', 'Congés payés', '#198754', 10),
  ('00000000-0000-0000-0000-000000000001', 'RTT', 'RTT', '#0d6efd', 20),
  ('00000000-0000-0000-0000-000000000001', 'UNPAID', 'Sans solde', '#6c757d', 30);
