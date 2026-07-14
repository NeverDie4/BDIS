-- Complete the M18 performance-recognition model without changing historical migrations.

ALTER TABLE `perf_standard`
  DROP INDEX `uk_perf_standard_standard_no`,
  ADD COLUMN `standard_version` INT NOT NULL DEFAULT 1 COMMENT '标准版本号' AFTER `standard_no`,
  ADD COLUMN `lifecycle_status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '生命周期：draft、published、disabled' AFTER `status`,
  ADD COLUMN `level_rule` TEXT NULL COMMENT '等级或等级认定规则' AFTER `score_rule`,
  ADD COLUMN `effective_from` DATETIME NULL DEFAULT NULL COMMENT '适用开始时间' AFTER `level_rule`,
  ADD COLUMN `effective_to` DATETIME NULL DEFAULT NULL COMMENT '适用结束时间' AFTER `effective_from`,
  ADD COLUMN `material_required` TINYINT NOT NULL DEFAULT 1 COMMENT '是否要求佐证材料：1 是，0 否' AFTER `effective_to`,
  ADD COLUMN `min_material_count` INT NOT NULL DEFAULT 1 COMMENT '最少佐证材料数量' AFTER `material_required`,
  ADD COLUMN `published_at` DATETIME NULL DEFAULT NULL COMMENT '发布时间' AFTER `min_material_count`,
  ADD COLUMN `published_by` BIGINT NULL DEFAULT NULL COMMENT '发布人 ID' AFTER `published_at`,
  ADD UNIQUE KEY `uk_perf_standard_no_version` (`standard_no`, `standard_version`),
  ADD KEY `idx_perf_standard_lifecycle` (`lifecycle_status`),
  ADD KEY `idx_perf_standard_effective` (`effective_from`, `effective_to`);

UPDATE `perf_standard`
SET `lifecycle_status` = CASE WHEN `status` = 1 THEN 'published' ELSE 'disabled' END,
    `effective_from` = COALESCE(`created_at`, CURRENT_TIMESTAMP),
    `published_at` = CASE WHEN `status` = 1 THEN COALESCE(`created_at`, CURRENT_TIMESTAMP) ELSE NULL END,
    `published_by` = CASE WHEN `status` = 1 THEN `created_by` ELSE NULL END;

ALTER TABLE `perf_record`
  ADD COLUMN `performance_level` VARCHAR(50) NOT NULL DEFAULT 'unspecified' COMMENT '业绩等级' AFTER `performance_type`,
  ADD COLUMN `occurred_at` DATETIME NULL DEFAULT NULL COMMENT '业绩发生时间' AFTER `performance_level`,
  ADD COLUMN `source_name_snapshot` VARCHAR(200) NULL DEFAULT NULL COMMENT '来源名称快照' AFTER `source_id`,
  ADD COLUMN `standard_no_snapshot` VARCHAR(64) NULL DEFAULT NULL COMMENT '认定标准编号快照' AFTER `standard_id`,
  ADD COLUMN `standard_version_snapshot` INT NULL DEFAULT NULL COMMENT '认定标准版本快照' AFTER `standard_no_snapshot`,
  ADD COLUMN `standard_name_snapshot` VARCHAR(150) NULL DEFAULT NULL COMMENT '认定标准名称快照' AFTER `standard_version_snapshot`,
  ADD COLUMN `standard_rule_snapshot` LONGTEXT NULL COMMENT '认定规则快照' AFTER `standard_name_snapshot`,
  ADD KEY `idx_perf_record_level` (`performance_level`),
  ADD KEY `idx_perf_record_occurred_at` (`occurred_at`);

UPDATE `perf_record` record
LEFT JOIN `perf_standard` standard ON standard.id = record.standard_id
SET record.`occurred_at` = COALESCE(record.`created_at`, CURRENT_TIMESTAMP),
    record.`standard_no_snapshot` = standard.`standard_no`,
    record.`standard_version_snapshot` = standard.`standard_version`,
    record.`standard_name_snapshot` = standard.`standard_name`,
    record.`standard_rule_snapshot` = standard.`score_rule`
WHERE record.`standard_id` IS NOT NULL;

CREATE TABLE `perf_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `performance_id` BIGINT NOT NULL COMMENT '业绩 ID',
  `user_id` BIGINT NOT NULL COMMENT '参与用户 ID',
  `participant_role` VARCHAR(50) NOT NULL DEFAULT 'participant' COMMENT '参与角色',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否负责人：1 是，0 否',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 停用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_perf_participant_performance` (`performance_id`, `is_deleted`),
  KEY `idx_perf_participant_user` (`user_id`, `is_deleted`),
  KEY `idx_perf_participant_primary` (`performance_id`, `is_primary`, `is_deleted`)
) ENGINE=InnoDB COMMENT='业绩参与人表；保存负责人和参与人，用于业绩归属与参与统计。';

INSERT INTO `perf_participant` (
  `performance_id`, `user_id`, `participant_role`, `sort_order`, `is_primary`,
  `status`, `created_at`, `updated_at`, `created_by`, `updated_by`, `remark`
)
SELECT record.`id`, record.`user_id`, 'owner', 0, 1,
       1, record.`created_at`, record.`updated_at`, record.`created_by`, record.`updated_by`, '迁移生成的负责人参与记录'
FROM `perf_record` record
WHERE NOT EXISTS (
  SELECT 1 FROM `perf_participant` participant
  WHERE participant.`performance_id` = record.`id` AND participant.`is_deleted` = 0
);
