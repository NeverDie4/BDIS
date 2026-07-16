-- Development-only seed for the performance-recognition review workflow.
--
-- It assigns the local sample student (user-2) and reviewer (user-3) to one
-- department, so the REVIEWER role's department data scope can review the
-- student's submitted performance records without weakening production scope rules.
--
-- Re-running this script is safe. It intentionally does not create or change
-- production users and is not a Flyway migration.

SET @performance_demo_org_id := (
  SELECT organization_id
  FROM sys_user
  WHERE username = 'user-2' AND is_deleted = 0
  LIMIT 1
);

INSERT INTO sys_department
  (department_no, department_name, organization_id, parent_id, sort_order, status, is_deleted, created_at, updated_at, version, remark)
SELECT
  'DEV_PERFORMANCE_REVIEW',
  '本地业绩认定演示部门',
  @performance_demo_org_id,
  0,
  0,
  1,
  0,
  NOW(),
  NOW(),
  0,
  'performance review development seed'
WHERE @performance_demo_org_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_department
    WHERE department_no = 'DEV_PERFORMANCE_REVIEW'
  );

SET @performance_demo_department_id := (
  SELECT id
  FROM sys_department
  WHERE department_no = 'DEV_PERFORMANCE_REVIEW' AND is_deleted = 0
  LIMIT 1
);

UPDATE sys_user
SET organization_id = @performance_demo_org_id,
    department_id = @performance_demo_department_id,
    updated_at = NOW(),
    remark = 'performance review development seed'
WHERE username IN ('user-2', 'user-3')
  AND is_deleted = 0
  AND @performance_demo_org_id IS NOT NULL
  AND @performance_demo_department_id IS NOT NULL;
