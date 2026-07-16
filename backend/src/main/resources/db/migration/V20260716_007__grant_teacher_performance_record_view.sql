-- Teachers maintain performance standards and need read access to enter the performance workspace.
-- Keep authoring and auditing permissions unchanged: record-level scope still applies on reads.
INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.`id`, permission.`id`
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.`permission_code` = 'performance:record:view'
WHERE role.`role_code` = 'TEACHER';
