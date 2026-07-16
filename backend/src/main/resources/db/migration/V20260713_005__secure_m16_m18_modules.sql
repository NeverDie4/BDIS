-- M16-M18 permissions and archive uniqueness, ordered after merged branch migrations.
INSERT IGNORE INTO `auth_permission`
(`permission_code`, `permission_name`, `permission_type`, `description`, `status`)
VALUES
('evaluation:standard:view', '查看评价标准', 'api', '查看 M16 评价标准', 1),
('evaluation:standard:manage', '维护评价标准', 'api', '新增和修改 M16 评价标准', 1),
('evaluation:task:view', '查看评价任务', 'api', '按数据范围查看 M16 评价任务', 1),
('evaluation:task:create', '创建评价任务', 'api', '创建 M16 评价任务', 1),
('evaluation:score:create', '评价评分', 'api', '录入或修改本人评分', 1),
('evaluation:result:confirm', '确认评价结果', 'api', '计算并确认 M16 评价结果', 1),
('declaration:application:view', '查看申报', 'api', '按数据范围查看 M17 申报', 1),
('declaration:application:create', '创建申报', 'api', '创建本人 M17 申报', 1),
('declaration:application:update', '修改申报', 'api', '修改本人 M17 申报和材料', 1),
('declaration:application:submit', '提交申报', 'api', '提交本人 M17 申报', 1),
('declaration:application:audit', '审核申报', 'api', '审核数据范围内的 M17 申报', 1),
('declaration:application:archive', '申报归档', 'api', '归档审核通过的 M17 申报', 1),
('performance:standard:view', '查看业绩标准', 'api', '查看 M18 业绩认定标准', 1),
('performance:standard:manage', '维护业绩标准', 'api', '新增和修改 M18 业绩认定标准', 1),
('performance:record:view', '查看业绩', 'api', '按数据范围查看 M18 业绩', 1),
('performance:record:create', '创建业绩', 'api', '创建本人 M18 业绩', 1),
('performance:record:update', '修改业绩', 'api', '修改本人 M18 业绩和材料', 1),
('performance:record:submit', '提交业绩', 'api', '提交本人 M18 业绩', 1),
('performance:record:audit', '审核业绩', 'api', '审核数据范围内的 M18 业绩', 1);

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
CROSS JOIN `auth_permission` permission
WHERE role.role_code = 'ADMIN'
  AND (permission.permission_code LIKE 'evaluation:%'
    OR permission.permission_code LIKE 'declaration:%'
    OR permission.permission_code LIKE 'performance:%');

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'declaration:application:view',
    'declaration:application:create',
    'declaration:application:update',
    'declaration:application:submit',
    'performance:standard:view',
    'performance:record:view',
    'performance:record:create',
    'performance:record:update',
    'performance:record:submit')
WHERE role.role_code = 'STUDENT';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'evaluation:standard:view',
    'evaluation:standard:manage',
    'evaluation:task:view',
    'evaluation:task:create',
    'evaluation:score:create',
    'evaluation:result:confirm',
    'performance:standard:view',
    'performance:standard:manage')
WHERE role.role_code = 'TEACHER';

INSERT IGNORE INTO `rel_role_permission` (`role_id`, `permission_id`)
SELECT role.id, permission.id
FROM `auth_role` role
JOIN `auth_permission` permission
  ON permission.permission_code IN (
    'declaration:application:view',
    'declaration:application:audit',
    'declaration:application:archive',
    'performance:standard:view',
    'performance:record:view',
    'performance:record:audit')
WHERE role.role_code = 'REVIEWER';

SET @drop_eval_archive_index_sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE `eval_archive` DROP INDEX `idx_eval_archive_application_id`',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'eval_archive'
      AND index_name = 'idx_eval_archive_application_id'
);
PREPARE drop_eval_archive_index_stmt FROM @drop_eval_archive_index_sql;
EXECUTE drop_eval_archive_index_stmt;
DEALLOCATE PREPARE drop_eval_archive_index_stmt;

SET @add_eval_archive_unique_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE `eval_archive` ADD UNIQUE KEY `uk_eval_archive_application_id` (`application_id`)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'eval_archive'
      AND index_name = 'uk_eval_archive_application_id'
);
PREPARE add_eval_archive_unique_stmt FROM @add_eval_archive_unique_sql;
EXECUTE add_eval_archive_unique_stmt;
DEALLOCATE PREPARE add_eval_archive_unique_stmt;
