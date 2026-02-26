-- ============================================================
-- V12: Configuration IA par tenant (Ollama / OpenAI compatible)
-- ============================================================

CREATE TABLE tenant_ai_config (
    tenant_id   VARCHAR(36)  NOT NULL,
    base_url    VARCHAR(500) NOT NULL,
    api_key     VARCHAR(500) NULL,
    model       VARCHAR(100) NULL,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tenant_ai_config PRIMARY KEY (tenant_id),
    CONSTRAINT fk_taic_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);
