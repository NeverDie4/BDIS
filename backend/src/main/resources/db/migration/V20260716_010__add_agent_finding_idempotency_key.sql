-- 为确定性诊断发现项增加数据库级幂等约束，不改写历史发现内容。
-- 手工回滚仅需在确认无并发诊断后删除唯一索引和生成列；本迁移不会自动 DROP。

ALTER TABLE `assistant_agent_finding`
  ADD COLUMN `finding_guard` VARCHAR(320) GENERATED ALWAYS AS (
    CONCAT(
      `agent_task_id`, ':', `finding_type`, ':',
      COALESCE(`target_type`, ''), ':', COALESCE(`target_id`, 0)
    )
  ) STORED,
  ADD UNIQUE KEY `uk_agent_finding_guard` (`finding_guard`);
