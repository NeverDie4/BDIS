-- 本迁移只新增 Agent 工具审计日志，不包含演示数据。
-- 手工回滚前应先备份审计数据，再由运维按外键逆序删除本表；迁移不会自动 DROP TABLE。

CREATE TABLE `assistant_agent_tool_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_task_id` BIGINT NOT NULL,
  `step_id` BIGINT NULL,
  `user_id` BIGINT NOT NULL,
  `session_id` VARCHAR(128) NULL,
  `tool_name` VARCHAR(128) NOT NULL,
  `request_summary` VARCHAR(1000) NULL,
  `input_hash` VARCHAR(64) NULL,
  `output_summary` TEXT NULL,
  `output_json` LONGTEXT NULL,
  `success` TINYINT NOT NULL,
  `error_code` VARCHAR(64) NULL,
  `error_message` VARCHAR(1000) NULL,
  `time_cost_ms` BIGINT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_tool_task` (`agent_task_id`),
  KEY `idx_agent_tool_step` (`step_id`),
  KEY `idx_agent_tool_user_time` (`user_id`, `create_time`),
  KEY `idx_agent_tool_name` (`tool_name`),
  CONSTRAINT `fk_agent_tool_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`),
  CONSTRAINT `fk_agent_tool_step`
    FOREIGN KEY (`step_id`) REFERENCES `assistant_agent_step` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 只读业务工具调用审计日志';
