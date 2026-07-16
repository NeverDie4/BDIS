-- Restore read permissions required by the teaching page's user-scoped views.

INSERT IGNORE INTO `auth_permission`
    (`permission_code`, `permission_name`, `permission_type`, `api_path`, `request_method`, `description`, `status`)
VALUES
    ('edu:course:enrollment:list', 'edu:course:enrollment:list', 'api', '/api/courses/enrollments', 'GET', 'View current user course enrollments', 1),
    ('research:project:list', 'research:project:list', 'api', '/api/research-projects', 'GET', 'View scoped research projects', 1),
    ('research:project:detail', 'research:project:detail', 'api', '/api/research-projects/{id}', 'GET', 'View scoped research project details', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'STUDENT'
  AND p.permission_code IN (
      'edu:course:enrollment:list',
      'research:project:list',
      'research:project:detail'
  );

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code = 'TEACHER'
  AND p.permission_code IN (
      'research:project:list',
      'research:project:detail'
  );
