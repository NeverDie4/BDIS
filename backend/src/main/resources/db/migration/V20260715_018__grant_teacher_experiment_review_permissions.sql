-- Grant teachers read and review access to submitted experiment reports.
-- Additive and idempotent; applies the permissions already seeded by M14.
INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'TEACHER'
  AND p.permission_code IN (
    'edu:teaching:view',
    'edu:experiment-record:list',
    'edu:experiment-record:detail',
    'edu:experiment-record:archive'
  );
