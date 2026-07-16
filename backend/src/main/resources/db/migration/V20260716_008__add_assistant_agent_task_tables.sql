-- 本迁移只新增 Agent 持久化底座，不包含演示数据。
-- 手工回滚顺序：assistant_agent_action、assistant_agent_finding、assistant_agent_step、assistant_agent_task。
-- 不在迁移中自动执行 DROP TABLE，已产生的 Agent 审计数据应先备份。

CREATE TABLE `assistant_agent_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_no` VARCHAR(64) NOT NULL,
  `session_id` VARCHAR(64) NULL,
  `user_id` BIGINT NOT NULL,
  `goal_type` VARCHAR(64) NOT NULL,
  `goal_text` VARCHAR(1000) NOT NULL,
  `target_type` VARCHAR(64) NULL,
  `target_id` BIGINT NULL,
  `collection_task_id` BIGINT NULL,
  `species_id` BIGINT NULL,
  `status` VARCHAR(32) NOT NULL,
  `current_phase` VARCHAR(64) NULL,
  `progress_percent` INT NOT NULL DEFAULT 0,
  `context_json` LONGTEXT NULL,
  `result_summary` TEXT NULL,
  `error_code` VARCHAR(64) NULL,
  `error_message` VARCHAR(1000) NULL,
  `version` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `start_time` DATETIME NULL,
  `finish_time` DATETIME NULL,
  `cancel_time` DATETIME NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `active_task_key` VARCHAR(255) GENERATED ALWAYS AS (
    CASE
      WHEN `is_deleted` = 0
       AND `collection_task_id` IS NOT NULL
       AND `status` IN (
         'CREATED', 'PLANNING', 'RUNNING', 'WAITING_CONFIRMATION',
         'WAITING_FIELD_DATA', 'REANALYZING'
       )
      THEN CONCAT(`user_id`, ':', `collection_task_id`, ':', `goal_type`)
      ELSE NULL
    END
  ) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assistant_agent_task_no` (`task_no`),
  UNIQUE KEY `uk_agent_task_active_key` (`active_task_key`),
  KEY `idx_agent_task_user_status` (`user_id`, `status`),
  KEY `idx_agent_task_collection_task` (`collection_task_id`),
  KEY `idx_agent_task_session` (`session_id`),
  CONSTRAINT `fk_agent_task_session`
    FOREIGN KEY (`user_id`, `session_id`)
    REFERENCES `herb_ai_chat_session` (`user_id`, `session_id`),
  CONSTRAINT `fk_agent_task_collection_task`
    FOREIGN KEY (`collection_task_id`) REFERENCES `herb_collection_task` (`id`),
  CONSTRAINT `chk_agent_task_goal_type` CHECK (`goal_type` IN (
    'DIGITAL_TWIN_RESEARCH', 'TASK_COMPLETENESS_CHECK', 'EVIDENCE_GAP_ANALYSIS',
    'FOLLOW_UP_COLLECTION_PLAN', 'DIGITAL_ARCHIVE_PREPARATION'
  )),
  CONSTRAINT `chk_agent_task_status` CHECK (`status` IN (
    'CREATED', 'PLANNING', 'RUNNING', 'WAITING_CONFIRMATION',
    'WAITING_FIELD_DATA', 'REANALYZING', 'COMPLETED', 'FAILED', 'CANCELLED'
  )),
  CONSTRAINT `chk_agent_task_progress` CHECK (`progress_percent` BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本草数字孪生科研 Agent 长期任务';

CREATE TABLE `assistant_agent_step` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_task_id` BIGINT NOT NULL,
  `step_no` INT NOT NULL,
  `step_type` VARCHAR(64) NOT NULL,
  `step_name` VARCHAR(255) NOT NULL,
  `description` VARCHAR(1000) NULL,
  `status` VARCHAR(32) NOT NULL,
  `input_snapshot` LONGTEXT NULL,
  `output_summary` TEXT NULL,
  `output_json` LONGTEXT NULL,
  `error_code` VARCHAR(64) NULL,
  `error_message` VARCHAR(1000) NULL,
  `retry_count` INT NOT NULL DEFAULT 0,
  `max_retry_count` INT NOT NULL DEFAULT 0,
  `start_time` DATETIME NULL,
  `finish_time` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `running_guard` TINYINT GENERATED ALWAYS AS (
    CASE WHEN `status` = 'RUNNING' THEN 1 ELSE NULL END
  ) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_step_task_no` (`agent_task_id`, `step_no`),
  UNIQUE KEY `uk_agent_step_single_running` (`agent_task_id`, `running_guard`),
  KEY `idx_agent_step_task_status` (`agent_task_id`, `status`),
  CONSTRAINT `fk_agent_step_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`),
  CONSTRAINT `chk_agent_step_type` CHECK (`step_type` IN (
    'LOAD_CONTEXT', 'INSPECT_TASK', 'ANALYZE_TIMELINE', 'ANALYZE_EVIDENCE',
    'GENERATE_COLLECTION_PLAN', 'WAIT_FOR_CONFIRMATION', 'CREATE_COLLECTION_TASK',
    'WAIT_FOR_FIELD_DATA', 'REANALYZE', 'PREPARE_DIGITAL_ARCHIVE',
    'VERIFY_ARCHIVE', 'COMPLETE_REPORT'
  )),
  CONSTRAINT `chk_agent_step_status` CHECK (`status` IN (
    'PENDING', 'RUNNING', 'WAITING', 'SUCCEEDED', 'FAILED', 'SKIPPED', 'CANCELLED'
  )),
  CONSTRAINT `chk_agent_step_retry` CHECK (
    `retry_count` >= 0 AND `max_retry_count` >= 0 AND `retry_count` <= `max_retry_count`
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 持久化工作流步骤';

CREATE TABLE `assistant_agent_finding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_task_id` BIGINT NOT NULL,
  `step_id` BIGINT NULL,
  `finding_type` VARCHAR(64) NOT NULL,
  `severity` VARCHAR(16) NOT NULL,
  `target_type` VARCHAR(64) NULL,
  `target_id` BIGINT NULL,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `evidence_json` LONGTEXT NULL,
  `suggestion` TEXT NULL,
  `status` VARCHAR(32) NOT NULL,
  `resolved_time` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_finding_task_status` (`agent_task_id`, `status`),
  KEY `idx_agent_finding_step` (`step_id`),
  CONSTRAINT `fk_agent_finding_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`),
  CONSTRAINT `fk_agent_finding_step`
    FOREIGN KEY (`step_id`) REFERENCES `assistant_agent_step` (`id`),
  CONSTRAINT `chk_agent_finding_severity`
    CHECK (`severity` IN ('INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
  CONSTRAINT `chk_agent_finding_status`
    CHECK (`status` IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'IGNORED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 发现的问题、异常和证据缺口';

CREATE TABLE `assistant_agent_action` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_task_id` BIGINT NOT NULL,
  `step_id` BIGINT NULL,
  `action_type` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(64) NULL,
  `target_id` BIGINT NULL,
  `action_name` VARCHAR(255) NOT NULL,
  `action_description` TEXT NULL,
  `payload_json` LONGTEXT NULL,
  `risk_level` VARCHAR(16) NOT NULL,
  `need_confirm` TINYINT NOT NULL,
  `status` VARCHAR(32) NOT NULL,
  `requested_time` DATETIME NOT NULL,
  `confirmed_by` BIGINT NULL,
  `confirmed_time` DATETIME NULL,
  `rejected_time` DATETIME NULL,
  `executed_time` DATETIME NULL,
  `result_summary` TEXT NULL,
  `error_message` VARCHAR(1000) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_agent_action_task_status` (`agent_task_id`, `status`),
  KEY `idx_agent_action_step` (`step_id`),
  CONSTRAINT `fk_agent_action_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`),
  CONSTRAINT `fk_agent_action_step`
    FOREIGN KEY (`step_id`) REFERENCES `assistant_agent_step` (`id`),
  CONSTRAINT `chk_agent_action_risk`
    CHECK (`risk_level` IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
  CONSTRAINT `chk_agent_action_confirm` CHECK (`need_confirm` IN (0, 1)),
  CONSTRAINT `chk_agent_action_status` CHECK (`status` IN (
    'PROPOSED', 'WAITING_CONFIRMATION', 'CONFIRMED', 'REJECTED',
    'EXECUTING', 'SUCCEEDED', 'FAILED', 'CANCELLED'
  ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 待确认或待执行动作';
