-- Preserve existing reviewer scope rules. Add the role's default department scope only when absent.
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
    role.id,
    'herb_growth_record',
    'department',
    NULL,
    NULL,
    NULL,
    1,
    0,
    'growth-reviewer-scope-migration-20260713-004'
FROM auth_role role
WHERE role.role_code = 'REVIEWER'
  AND role.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM auth_data_scope scope
      WHERE scope.role_id = role.id
        AND scope.resource_type = 'herb_growth_record'
        AND scope.is_deleted = 0
  );
