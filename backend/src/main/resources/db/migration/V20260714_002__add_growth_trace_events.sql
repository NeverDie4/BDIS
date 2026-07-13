ALTER TABLE `herb_growth_review_record`
  ADD COLUMN `reviewer_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作人姓名快照' AFTER `reviewer_id`,
  ADD COLUMN `reviewer_role` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作人角色快照' AFTER `reviewer_name`;

CREATE TABLE IF NOT EXISTS `herb_growth_trace_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `record_id` BIGINT NOT NULL COMMENT '生长采集记录 ID',
  `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型：created、updated、submitted、approved、rejected、resubmitted、archived',
  `event_title` VARCHAR(100) NOT NULL COMMENT '事件标题',
  `event_content` VARCHAR(500) NULL DEFAULT NULL COMMENT '事件内容',
  `before_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作前状态',
  `after_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作后状态',
  `operator_id` BIGINT NULL DEFAULT NULL COMMENT '操作人 ID',
  `operator_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作人姓名快照',
  `operator_role` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作人角色快照',
  `event_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `metadata_json` TEXT NULL COMMENT '事件元数据 JSON',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_herb_growth_trace_event_record_id` (`record_id`),
  KEY `idx_herb_growth_trace_event_event_time` (`event_time`)
) ENGINE=InnoDB COMMENT='生长采集记录溯源事件表；保存创建、修改、提交、审核和归档事件。';
