-- Teachers supervise the complete growth workspace, independent of department membership.
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
    'teacher-all-growth-access',
    0
FROM auth_role r
WHERE r.role_code = 'TEACHER'
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
    remark = 'teacher-all-growth-access',
    version = auth_data_scope.version + 1;

-- Students must not see or directly access the Web growth workspace.
DELETE rp
FROM rel_role_permission rp
JOIN auth_role r ON r.id = rp.role_id
JOIN auth_permission p ON p.id = rp.permission_id
WHERE r.role_code = 'STUDENT'
  AND p.permission_code = 'growth:record:view';
