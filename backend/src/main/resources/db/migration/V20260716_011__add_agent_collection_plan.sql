-- 复测采集方案只保存草稿和确认状态，本迁移不创建任何真实采集任务或演示数据。
-- 手工回滚时应先确认无业务引用，再依次移除 action 唯一索引/生成列和方案表；本迁移不自动 DROP。

CREATE TABLE `assistant_agent_collection_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `plan_no` VARCHAR(64) NOT NULL,
  `agent_task_id` BIGINT NOT NULL,
  `source_step_id` BIGINT NULL,
  `research_round` INT NOT NULL,
  `collection_task_id` BIGINT NOT NULL,
  `species_id` BIGINT NULL,
  `base_id` BIGINT NULL,
  `objective` VARCHAR(1000) NOT NULL,
  `recommended_time_type` VARCHAR(32) NULL,
  `recommended_after_days` INT NULL,
  `recommended_start_time` DATETIME NULL,
  `recommended_end_time` DATETIME NULL,
  `required_metrics_json` LONGTEXT NULL,
  `required_images_json` LONGTEXT NULL,
  `optional_items_json` LONGTEXT NULL,
  `completion_criteria_json` LONGTEXT NULL,
  `source_findings_json` LONGTEXT NULL,
  `rationale` TEXT NULL,
  `uncertainty` TEXT NULL,
  `priority` VARCHAR(16) NULL,
  `plan_source` VARCHAR(32) NOT NULL,
  `prompt_version` VARCHAR(64) NULL,
  `model_name` VARCHAR(128) NULL,
  `status` VARCHAR(32) NOT NULL,
  `version` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  `confirm_time` DATETIME NULL,
  `expire_time` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_collection_plan_no` (`plan_no`),
  UNIQUE KEY `uk_agent_collection_round` (`agent_task_id`, `research_round`),
  KEY `idx_agent_collection_plan_task_status` (`agent_task_id`, `status`),
  CONSTRAINT `fk_agent_collection_plan_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 复测采集方案';

ALTER TABLE `assistant_agent_action`
  ADD COLUMN `active_plan_action_guard` BIGINT GENERATED ALWAYS AS (
    CASE
      WHEN `action_type` = 'CREATE_FOLLOW_UP_COLLECTION_TASK'
       AND `status` IN ('PROPOSED', 'WAITING_CONFIRMATION', 'CONFIRMED', 'EXECUTING')
      THEN `agent_task_id`
      ELSE NULL
    END
  ) STORED,
  ADD UNIQUE KEY `uk_agent_active_collection_plan_action` (`active_plan_action_guard`);
