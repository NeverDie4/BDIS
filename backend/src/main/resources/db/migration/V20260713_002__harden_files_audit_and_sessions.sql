-- Harden controlled file URLs, standardize audit results, and identify client devices.

UPDATE `herb_image` image
JOIN `sys_file_resource` file ON file.file_url = image.image_url
SET image.image_url = CASE
  WHEN file.access_level = 'public' THEN CONCAT('/api/public-files/', file.id, '/content')
  ELSE CONCAT('/api/files/', file.id, '/content')
END
WHERE image.image_url LIKE '/api/files/uploads/%';

UPDATE `herb_atlas` atlas
JOIN `sys_file_resource` file ON file.file_url = atlas.image_url
SET atlas.image_url = CASE
  WHEN file.access_level = 'public' THEN CONCAT('/api/public-files/', file.id, '/content')
  ELSE CONCAT('/api/files/', file.id, '/content')
END
WHERE atlas.image_url LIKE '/api/files/uploads/%';

UPDATE `sys_file_resource`
SET `file_url` = CASE
      WHEN `access_level` = 'public' THEN CONCAT('/api/public-files/', `id`, '/content')
      ELSE CONCAT('/api/files/', `id`, '/content')
    END,
    `thumbnail_url` = CASE
      WHEN `thumbnail_url` IS NULL THEN NULL
      WHEN `access_level` = 'public' THEN CONCAT('/api/public-files/', `id`, '/content')
      ELSE CONCAT('/api/files/', `id`, '/content')
    END;

UPDATE `log_login`
SET `login_result` = UPPER(`login_result`)
WHERE `login_result` IN ('success', 'failed');

ALTER TABLE `auth_user_session`
  ADD COLUMN `device_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '客户端持久设备标识' AFTER `user_id`,
  ADD KEY `idx_auth_user_session_user_device` (`user_id`, `device_id`, `session_status`);

DELETE relation
FROM `rel_role_permission` relation
JOIN `auth_role` role ON role.id = relation.role_id
JOIN `auth_permission` permission ON permission.id = relation.permission_id
WHERE role.role_code IN ('TEACHER', 'STUDENT', 'COLLECTOR', 'REVIEWER')
  AND permission.permission_code = 'auth:dashboard:view';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission ON permission.permission_code = 'auth:dashboard:view'
WHERE role.role_code IN ('ADMIN', 'AUDITOR');
