-- Enforce a single active participant per user while preserving logically deleted history.

ALTER TABLE `perf_participant`
  ADD COLUMN `active_user_id` BIGINT
    GENERATED ALWAYS AS (CASE WHEN `is_deleted` = 0 THEN `user_id` ELSE NULL END) STORED
    COMMENT '用于约束同一业绩的有效参与人唯一性' AFTER `user_id`,
  ADD UNIQUE KEY `uk_perf_participant_active_user` (`performance_id`, `active_user_id`);
