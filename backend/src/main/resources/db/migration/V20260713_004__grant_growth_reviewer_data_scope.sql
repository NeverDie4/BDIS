-- Reviewers must be able to discover collection tasks and submitted growth records
-- even when the reviewer account is not assigned to an organization or department.
INSERT INTO auth_data_scope (
    role_id,
    resource_type,
    scope_type,
    organization_id,
    department_id,
    custom_rule,
    status,
    is_deleted,
    remark
)
SELECT
    id,
    'herb_growth_record',
    'all',
    NULL,
    NULL,
    NULL,
    1,
    0,
    'Reviewers can access growth records pending review'
FROM auth_role
WHERE role_code = 'REVIEWER'
  AND is_deleted = 0
ON DUPLICATE KEY UPDATE
    scope_type = 'all',
    organization_id = NULL,
    department_id = NULL,
    custom_rule = NULL,
    status = 1,
    is_deleted = 0,
    remark = 'Reviewers can access growth records pending review',
    updated_at = CURRENT_TIMESTAMP;
