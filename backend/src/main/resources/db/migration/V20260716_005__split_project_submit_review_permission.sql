-- Project owners submit projects for review; reviewers execute review decisions.
INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `api_path`, `request_method`, `description`, `status`)
VALUES
('research:project:submit-review', 'research:project:submit-review', 'api',
 '/api/research-projects/{id}/submit-review', 'POST', 'Submit research project for review', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `auth_role` r
CROSS JOIN `auth_permission` p
WHERE r.role_code IN ('ADMIN', 'RESEARCHER')
  AND p.permission_code = 'research:project:submit-review';
