-- Development-only mobile collection seed data.
-- Safe for a clean Flyway-managed schema: it creates its own dev collector,
-- herb species, bases, tasks, and batches, then references them by business code.
--
-- Dev login:
--   username: collector_dev
--   password: password

INSERT INTO `sys_user`
(`user_no`, `username`, `password_hash`, `real_name`, `user_type`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
VALUES
('DEV_COLLECTOR_001', 'collector_dev', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '开发采集员', 'collector', 1, 0, NOW(), NOW(), 0, 'mobile dev seed')
ON DUPLICATE KEY UPDATE
  `real_name` = VALUES(`real_name`),
  `user_type` = VALUES(`user_type`),
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW(),
  `remark` = VALUES(`remark`);

INSERT IGNORE INTO `rel_user_role` (`user_id`, `role_id`, `created_at`, `remark`)
SELECT user.id, role.id, NOW(), 'mobile dev seed'
FROM `sys_user` user
JOIN `auth_role` role ON role.role_code = 'COLLECTOR'
WHERE user.username = 'collector_dev';

INSERT INTO `herb_species`
(`herb_no`, `herb_name`, `medicinal_part`, `origin_area`, `description`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
VALUES
('DEV_HERB_HUANGLIAN', '黄连', '根茎', '重庆石柱', '移动端开发测试药材', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_HERB_DANGSHEN', '党参', '根', '甘肃岷县', '移动端开发测试药材', 1, 0, NOW(), NOW(), 0, 'mobile dev seed')
ON DUPLICATE KEY UPDATE
  `herb_name` = VALUES(`herb_name`),
  `medicinal_part` = VALUES(`medicinal_part`),
  `origin_area` = VALUES(`origin_area`),
  `description` = VALUES(`description`),
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW(),
  `remark` = VALUES(`remark`);

INSERT INTO `herb_base`
(`base_no`, `base_name`, `base_type`, `address`, `contact_name`, `description`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
VALUES
('DEV_BASE_SHIZHU', '石柱黄连开发测试基地', 'planting', '重庆市石柱县', '开发采集员', '移动端开发测试基地', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_BASE_MINXIAN', '岷县党参开发测试基地', 'planting', '甘肃省岷县', '开发采集员', '移动端开发测试基地', 1, 0, NOW(), NOW(), 0, 'mobile dev seed')
ON DUPLICATE KEY UPDATE
  `base_name` = VALUES(`base_name`),
  `base_type` = VALUES(`base_type`),
  `address` = VALUES(`address`),
  `contact_name` = VALUES(`contact_name`),
  `description` = VALUES(`description`),
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW(),
  `remark` = VALUES(`remark`);

SET @dev_collector_id := (SELECT id FROM `sys_user` WHERE username = 'collector_dev' AND is_deleted = 0 LIMIT 1);
SET @huanglian_species_id := (SELECT id FROM `herb_species` WHERE herb_no = 'DEV_HERB_HUANGLIAN' AND is_deleted = 0 LIMIT 1);
SET @dangshen_species_id := (SELECT id FROM `herb_species` WHERE herb_no = 'DEV_HERB_DANGSHEN' AND is_deleted = 0 LIMIT 1);
SET @shizhu_base_id := (SELECT id FROM `herb_base` WHERE base_no = 'DEV_BASE_SHIZHU' AND is_deleted = 0 LIMIT 1);
SET @minxian_base_id := (SELECT id FROM `herb_base` WHERE base_no = 'DEV_BASE_MINXIAN' AND is_deleted = 0 LIMIT 1);

INSERT INTO `herb_collection_task`
(`task_code`, `task_name`, `species_id`, `species_name`, `base_id`, `base_name`, `collect_place`, `planned_start_time`, `planned_end_time`, `collector_id`, `collector_name`, `task_status`, `description`, `status`, `is_deleted`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`, `remark`)
VALUES
('DEV_MOBILE_TASK_HUANGLIAN', '移动端黄连开发采集任务', @huanglian_species_id, '黄连', @shizhu_base_id, '石柱黄连开发测试基地', '重庆市石柱县开发样方 A-01', '2026-07-11 08:30:00', '2026-07-13 18:00:00', @dev_collector_id, '开发采集员', 'published', '用于移动端任务、批次、上传和识别联调。', 1, 0, NOW(), NOW(), @dev_collector_id, @dev_collector_id, 0, 'mobile dev seed'),
('DEV_MOBILE_TASK_DANGSHEN', '移动端党参开发观察任务', @dangshen_species_id, '党参', @minxian_base_id, '岷县党参开发测试基地', '甘肃省岷县开发样方 B-03', '2026-07-10 09:00:00', '2026-07-15 17:30:00', @dev_collector_id, '开发采集员', 'in_progress', '用于移动端批次详情和状态展示联调。', 1, 0, NOW(), NOW(), @dev_collector_id, @dev_collector_id, 0, 'mobile dev seed')
ON DUPLICATE KEY UPDATE
  `task_name` = VALUES(`task_name`),
  `species_id` = VALUES(`species_id`),
  `species_name` = VALUES(`species_name`),
  `base_id` = VALUES(`base_id`),
  `base_name` = VALUES(`base_name`),
  `collect_place` = VALUES(`collect_place`),
  `planned_start_time` = VALUES(`planned_start_time`),
  `planned_end_time` = VALUES(`planned_end_time`),
  `collector_id` = VALUES(`collector_id`),
  `collector_name` = VALUES(`collector_name`),
  `task_status` = VALUES(`task_status`),
  `description` = VALUES(`description`),
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW(),
  `updated_by` = VALUES(`updated_by`),
  `remark` = VALUES(`remark`);

SET @huanglian_task_id := (SELECT id FROM `herb_collection_task` WHERE task_code = 'DEV_MOBILE_TASK_HUANGLIAN' AND is_deleted = 0 LIMIT 1);
SET @dangshen_task_id := (SELECT id FROM `herb_collection_task` WHERE task_code = 'DEV_MOBILE_TASK_DANGSHEN' AND is_deleted = 0 LIMIT 1);

INSERT INTO `herb_batch`
(`batch_code`, `batch_name`, `task_id`, `species_id`, `species_name`, `base_id`, `base_name`, `origin_place`, `collect_start_time`, `collect_end_time`, `harvest_time`, `production_date`, `batch_status`, `image_count`, `identified_count`, `reviewed_count`, `need_review_count`, `final_species_id`, `final_species_name`, `avg_similarity`, `quality_level`, `quality_score`, `evaluation_summary`, `trace_code`, `status`, `is_deleted`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`, `remark`)
VALUES
('DEV_MOBILE_BATCH_HUANGLIAN_001', '黄连移动端采集第 1 批', @huanglian_task_id, @huanglian_species_id, '黄连', @shizhu_base_id, '石柱黄连开发测试基地', '重庆市石柱县开发样方 A-01', '2026-07-11 09:10:00', NULL, NULL, '2026-07-11', 'collecting', 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, '现场采集中，待上传图片。', 'TRACE-DEV-MOBILE-HL-001', 1, 0, NOW(), NOW(), @dev_collector_id, @dev_collector_id, 0, 'mobile dev seed'),
('DEV_MOBILE_BATCH_DANGSHEN_001', '党参移动端观察第 1 批', @dangshen_task_id, @dangshen_species_id, '党参', @minxian_base_id, '岷县党参开发测试基地', '甘肃省岷县开发样方 B-03', '2026-07-10 09:25:00', '2026-07-10 10:40:00', NULL, '2026-07-10', 'submitted', 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, '已提交批次，用于移动端状态展示。', 'TRACE-DEV-MOBILE-DS-001', 1, 0, NOW(), NOW(), @dev_collector_id, @dev_collector_id, 0, 'mobile dev seed')
ON DUPLICATE KEY UPDATE
  `batch_name` = VALUES(`batch_name`),
  `task_id` = VALUES(`task_id`),
  `species_id` = VALUES(`species_id`),
  `species_name` = VALUES(`species_name`),
  `base_id` = VALUES(`base_id`),
  `base_name` = VALUES(`base_name`),
  `origin_place` = VALUES(`origin_place`),
  `collect_start_time` = VALUES(`collect_start_time`),
  `collect_end_time` = VALUES(`collect_end_time`),
  `production_date` = VALUES(`production_date`),
  `batch_status` = VALUES(`batch_status`),
  `evaluation_summary` = VALUES(`evaluation_summary`),
  `trace_code` = VALUES(`trace_code`),
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW(),
  `updated_by` = VALUES(`updated_by`),
  `remark` = VALUES(`remark`);
