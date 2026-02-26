-- ============================================================
-- V13: Phase 5 – Assistant / chatbot (audit des requêtes)
-- ============================================================

CREATE TABLE assistant_audit (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    VARCHAR(36)  NOT NULL,
    user_id      BIGINT       NOT NULL,
    question     TEXT         NOT NULL,
    answer_preview VARCHAR(500) NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_assistant_audit PRIMARY KEY (id),
    CONSTRAINT fk_assistant_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_assistant_audit_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_assistant_audit_tenant ON assistant_audit(tenant_id);
CREATE INDEX idx_assistant_audit_created ON assistant_audit(created_at DESC);
