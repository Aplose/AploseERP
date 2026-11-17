-- ============================================================
-- V14: align the schema with the JPA entities.
-- Production runs Hibernate with ddl-auto=validate, which refuses to start on
-- these differences; in dev/test, the pages reading these tables failed.
-- (Checked by SchemaValidationTest.)
-- ============================================================

-- updated_at expected by BaseEntity but never created
ALTER TABLE product_categories ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE accounting_entries ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ged_documents ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- int fields in the entities, SMALLINT in the schema
ALTER TABLE accounting_entry_lines MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE ged_documents MODIFY COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE leave_types MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE sales_pipeline_stages MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0;
