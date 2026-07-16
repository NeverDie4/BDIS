-- Agent 确认动作与真实业务对象关联底座；不插入演示数据，不自动创建采集任务。
-- 手工回滚前必须先确认无业务引用；本迁移不自动 DROP 表或列。

ALTER TABLE `assistant_agent_action`
  ADD COLUMN `version` INT NOT NULL DEFAULT 0 AFTER `error_message`;

CREATE TABLE `assistant_agent_business_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_task_id` BIGINT NOT NULL,
  `agent_action_id` BIGINT NULL,
  `collection_plan_id` BIGINT NULL,
  `relation_type` VARCHAR(64) NOT NULL,
  `business_type` VARCHAR(64) NOT NULL,
  `business_id` BIGINT NOT NULL,
  `business_no` VARCHAR(128) NULL,
  `create_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_business_relation`
    (`agent_task_id`, `relation_type`, `business_type`, `business_id`),
  UNIQUE KEY `uk_agent_action_relation`
    (`agent_action_id`, `relation_type`, `business_type`),
  KEY `idx_agent_business_plan` (`collection_plan_id`),
  CONSTRAINT `fk_agent_business_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`),
  CONSTRAINT `fk_agent_business_action`
    FOREIGN KEY (`agent_action_id`) REFERENCES `assistant_agent_action` (`id`),
  CONSTRAINT `fk_agent_business_plan`
    FOREIGN KEY (`collection_plan_id`) REFERENCES `assistant_agent_collection_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 与业务对象关联';

CREATE TABLE `assistant_agent_collection_requirement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `collection_plan_id` BIGINT NOT NULL,
  `collection_task_id` BIGINT NOT NULL,
  `requirement_type` VARCHAR(32) NOT NULL,
  `requirement_code` VARCHAR(64) NOT NULL,
  `requirement_name` VARCHAR(128) NOT NULL,
  `required` TINYINT NOT NULL,
  `min_count` INT NULL,
  `unit` VARCHAR(32) NULL,
  `guidance` VARCHAR(1000) NULL,
  `reason` VARCHAR(1000) NULL,
  `sort_order` INT NOT NULL,
  `create_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_collection_requirement`
    (`collection_plan_id`, `requirement_type`, `requirement_code`),
  KEY `idx_agent_requirement_task` (`collection_task_id`, `sort_order`),
  CONSTRAINT `fk_agent_requirement_plan`
    FOREIGN KEY (`collection_plan_id`) REFERENCES `assistant_agent_collection_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 复测采集要求';
