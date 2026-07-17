-- Reviewers must be able to inspect the complete growth queue before auditing records.
INSERT INTO auth_data_scope (
    role_id,
    resource_type,
    scope_type,
    organization_id,
    department_id,
    custom_rule,
    status,
    is_deleted,
    remark,
    version
)
SELECT
    r.id,
    'herb_growth_record',
    'all',
    NULL,
    NULL,
    NULL,
    1,
    0,
    'reviewer-all-growth-access',
    0
FROM auth_role r
WHERE r.role_code = 'REVIEWER'
  AND r.status = 1
ON DUPLICATE KEY UPDATE
    scope_type = 'all',
    organization_id = NULL,
    department_id = NULL,
    custom_rule = NULL,
    status = 1,
    is_deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL,
    updated_at = CURRENT_TIMESTAMP,
    remark = 'reviewer-all-growth-access',
    version = auth_data_scope.version + 1;

INSERT IGNORE INTO rel_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM auth_role r
JOIN auth_permission p
  ON p.permission_code IN ('growth:record:view', 'growth:record:audit')
 AND p.status = 1
WHERE r.role_code = 'REVIEWER'
  AND r.status = 1;
