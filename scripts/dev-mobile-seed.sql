-- Development-only mobile collection seed data.
-- This script intentionally does not create sys_user rows.
-- Initialize the administrator through POST /api/auth/bootstrap-admin first,
-- then create/login users through the normal auth/user workflow.

INSERT INTO `herb_collection_task`
(`task_code`, `task_name`, `species_id`, `species_name`, `base_id`, `base_name`, `collect_place`, `planned_start_time`, `planned_end_time`, `collector_id`, `collector_name`, `task_status`, `description`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
VALUES
('DEV_MOBILE_20260711_TASK_001', '移动端黄芪样方采集任务', 4, '黄芪', 20001, '重庆南川金佛山中药材基地', '重庆市南川区金佛山北坡样方 A-01', '2026-07-11 08:30:00', '2026-07-13 18:00:00', 2, '采集员A', 'published', '用于移动端任务列表、批次创建和图片上传联调的默认测试任务。', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_MOBILE_20260711_TASK_002', '移动端党参连续观测任务', 1, '党参', 20002, '甘肃岷县党参示范基地', '甘肃省定西市岷县梅川镇试验田 B-03', '2026-07-10 09:00:00', '2026-07-15 17:30:00', 2, '采集员A', 'in_progress', '包含多个批次状态，用于检查批次列表、详情和状态标签显示。', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_MOBILE_20260711_TASK_003', '移动端枸杞花期补采任务', 2, '枸杞', 20003, '重庆武隆枸杞种植基地', '重庆市武隆区仙女山镇样地 C-02', '2026-07-12 07:30:00', '2026-07-14 16:30:00', 2, '采集员A', 'published', '无批次任务，用于验证任务详情页空批次状态。', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_MOBILE_20260711_TASK_004', '移动端金银花历史采集任务', 5, '金银花', 20004, '浙江乐清金银花基地', '浙江省温州市乐清市大荆镇温室 D-06', '2026-07-01 08:00:00', '2026-07-03 18:00:00', 2, '采集员A', 'completed', '用于筛选已完成任务状态的历史样例。', 1, 0, NOW(), NOW(), 0, 'mobile dev seed')
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
  `remark` = VALUES(`remark`);

INSERT INTO `herb_batch`
(`batch_code`, `batch_name`, `task_id`, `species_id`, `species_name`, `base_id`, `base_name`, `origin_place`, `collect_start_time`, `collect_end_time`, `harvest_time`, `production_date`, `batch_status`, `image_count`, `identified_count`, `reviewed_count`, `need_review_count`, `final_species_id`, `final_species_name`, `avg_similarity`, `quality_level`, `quality_score`, `evaluation_summary`, `trace_code`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
VALUES
('DEV_MOBILE_20260711_BATCH_001', '黄芪根部样品第 1 批', (SELECT id FROM herb_collection_task WHERE task_code = 'DEV_MOBILE_20260711_TASK_001'), 4, '黄芪', 20001, '重庆南川金佛山中药材基地', '重庆市南川区金佛山北坡样方 A-01', '2026-07-11 09:10:00', NULL, NULL, '2026-07-11', 'collecting', 0, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, '移动端现场采集中，待上传图片。', 'TRACE-DEV-MOBILE-001', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_MOBILE_20260711_BATCH_002', '黄芪地上部样品第 2 批', (SELECT id FROM herb_collection_task WHERE task_code = 'DEV_MOBILE_20260711_TASK_001'), 4, '黄芪', 20001, '重庆南川金佛山中药材基地', '重庆市南川区金佛山北坡样方 A-02', '2026-07-11 10:20:00', '2026-07-11 11:05:00', NULL, '2026-07-11', 'submitted', 3, 2, 0, 1, 4, '黄芪', 0.892100, 'good', 86.5000, '已提交后台审核，识别结果整体可信。', 'TRACE-DEV-MOBILE-002', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_MOBILE_20260711_BATCH_003', '党参叶片观测第 1 批', (SELECT id FROM herb_collection_task WHERE task_code = 'DEV_MOBILE_20260711_TASK_002'), 1, '党参', 20002, '甘肃岷县党参示范基地', '甘肃省定西市岷县梅川镇试验田 B-03', '2026-07-10 09:25:00', '2026-07-10 10:40:00', NULL, '2026-07-10', 'reviewing', 5, 5, 2, 3, 1, '党参', 0.781300, 'normal', 72.0000, '图片较完整，仍需复核叶片局部特征。', 'TRACE-DEV-MOBILE-003', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_MOBILE_20260711_BATCH_004', '党参根茎采集第 2 批', (SELECT id FROM herb_collection_task WHERE task_code = 'DEV_MOBILE_20260711_TASK_002'), 1, '党参', 20002, '甘肃岷县党参示范基地', '甘肃省定西市岷县梅川镇试验田 B-04', '2026-07-11 14:00:00', NULL, NULL, '2026-07-11', 'collecting', 1, 0, 0, 0, NULL, NULL, NULL, NULL, NULL, '现场继续采集中，适合测试未完成批次数。', 'TRACE-DEV-MOBILE-004', 1, 0, NOW(), NOW(), 0, 'mobile dev seed'),
('DEV_MOBILE_20260711_BATCH_005', '金银花温室复核批次', (SELECT id FROM herb_collection_task WHERE task_code = 'DEV_MOBILE_20260711_TASK_004'), 5, '金银花', 20004, '浙江乐清金银花基地', '浙江省温州市乐清市大荆镇温室 D-06', '2026-07-02 08:40:00', '2026-07-02 12:10:00', '2026-07-03 10:00:00', '2026-07-03', 'confirmed', 4, 4, 4, 0, 5, '金银花', 0.934200, 'excellent', 94.0000, '历史批次已确认，用于已完成任务详情展示。', 'TRACE-DEV-MOBILE-005', 1, 0, NOW(), NOW(), 0, 'mobile dev seed')
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
  `harvest_time` = VALUES(`harvest_time`),
  `production_date` = VALUES(`production_date`),
  `batch_status` = VALUES(`batch_status`),
  `image_count` = VALUES(`image_count`),
  `identified_count` = VALUES(`identified_count`),
  `reviewed_count` = VALUES(`reviewed_count`),
  `need_review_count` = VALUES(`need_review_count`),
  `final_species_id` = VALUES(`final_species_id`),
  `final_species_name` = VALUES(`final_species_name`),
  `avg_similarity` = VALUES(`avg_similarity`),
  `quality_level` = VALUES(`quality_level`),
  `quality_score` = VALUES(`quality_score`),
  `evaluation_summary` = VALUES(`evaluation_summary`),
  `trace_code` = VALUES(`trace_code`),
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW(),
  `remark` = VALUES(`remark`);
