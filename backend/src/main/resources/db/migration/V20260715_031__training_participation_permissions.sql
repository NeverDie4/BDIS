-- Allow all teaching roles to view plan statistics and let teachers/students
-- submit their own participation without granting participant-management rights.

INSERT IGNORE INTO `auth_permission`
    (`permission_code`, `permission_name`, `permission_type`, `api_path`, `request_method`, `description`, `status`)
VALUES
    ('edu:training-record:join', 'edu:training-record:join', 'api',
     '/api/training-plans/{planId}/participants/me', 'POST',
     'Submit current user training participation', '1');

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER', 'TRAINER', 'STUDENT')
  AND p.permission_code = 'edu:training-record:join';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER', 'TRAINER', 'STUDENT', 'RESEARCHER', 'REVIEWER')
  AND p.permission_code = 'edu:training-summary:view';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('TEACHER', 'TRAINER', 'STUDENT', 'RESEARCHER', 'REVIEWER')
  AND p.permission_code IN (
    'edu:training-plan:list',
    'edu:training-plan:detail',
    'edu:training-item:list',
    'edu:training-record:list',
    'edu:training-record:detail',
    'edu:training-feedback:list'
  );
