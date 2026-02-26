-- ============================================================
-- V11: Phase 5 – IA (couche générique, permission)
-- ============================================================

INSERT INTO permissions (code, module, action, description) VALUES
  ('AI_USE', 'AI', 'USE', 'Use AI suggestions (e.g. line description)');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN' AND r.tenant_id = '00000000-0000-0000-0000-000000000001'
  AND p.code = 'AI_USE';
