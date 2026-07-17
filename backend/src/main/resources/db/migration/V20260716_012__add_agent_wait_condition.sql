-- Agent 现场数据等待条件持久化；事件丢失或服务重启后可通过补偿扫描恢复。
-- 手工回滚前必须确认没有 Agent 等待任务引用；本迁移不自动 DROP TABLE。

CREATE TABLE `assistant_agent_wait_condition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_task_id` BIGINT NOT NULL,
  `agent_step_id` BIGINT NOT NULL,
  `collection_plan_id` BIGINT NULL,
  `follow_up_task_id` BIGINT NOT NULL,
  `condition_type` VARCHAR(64) NOT NULL,
  `condition_json` LONGTEXT NOT NULL,
  `current_snapshot_json` LONGTEXT NULL,
  `status` VARCHAR(32) NOT NULL,
  `deadline` DATETIME NULL,
  `last_check_time` DATETIME NULL,
  `satisfied_time` DATETIME NULL,
  `check_count` INT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_wait_task_type` (`agent_task_id`, `condition_type`),
  KEY `idx_agent_wait_follow_status` (`follow_up_task_id`, `status`),
  KEY `idx_agent_wait_status_deadline` (`status`, `deadline`),
  CONSTRAINT `fk_agent_wait_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`),
  CONSTRAINT `fk_agent_wait_step`
    FOREIGN KEY (`agent_step_id`) REFERENCES `assistant_agent_step` (`id`),
  CONSTRAINT `fk_agent_wait_plan`
    FOREIGN KEY (`collection_plan_id`) REFERENCES `assistant_agent_collection_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 持久化等待条件';
