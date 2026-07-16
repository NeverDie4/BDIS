-- Agent 补采前后分析轮次；快照仅保存规范化摘要及哈希，不保存图片、特征向量或敏感人员信息。
-- 手工回滚前必须确认无研究轮次引用；本迁移不自动删除表。

CREATE TABLE `assistant_agent_analysis_round` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agent_task_id` BIGINT NOT NULL,
  `round_no` INT NOT NULL,
  `round_type` VARCHAR(32) NOT NULL,
  `source_collection_task_id` BIGINT NOT NULL,
  `follow_up_collection_task_id` BIGINT NULL,
  `baseline_snapshot` LONGTEXT NULL,
  `baseline_hash` VARCHAR(64) NULL,
  `current_snapshot` LONGTEXT NULL,
  `current_hash` VARCHAR(64) NULL,
  `change_summary` LONGTEXT NULL,
  `conclusion` TEXT NULL,
  `outcome` VARCHAR(64) NULL,
  `status` VARCHAR(32) NOT NULL,
  `start_time` DATETIME NULL,
  `finish_time` DATETIME NULL,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_analysis_round` (`agent_task_id`, `round_no`),
  KEY `idx_agent_analysis_task_status` (`agent_task_id`, `status`),
  CONSTRAINT `fk_agent_analysis_task`
    FOREIGN KEY (`agent_task_id`) REFERENCES `assistant_agent_task` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 分析轮次';
