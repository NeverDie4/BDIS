-- BDIS 答辩演示核心数据（本地开发库专用）。
--
-- 约束：
-- 1. 本文件位于 scripts/，不得移入 Flyway migration 目录。
-- 2. 应先执行 scripts/dev-research-agent-demo.sql，或直接使用 pnpm demo:defense:seed。
-- 3. 所有坐标、指标和结论均为 demo_seed，不代表现场实测或农学结论。
-- 4. 本文件不预造 Agent 任务、复测任务、证据链或公开二维码；这些仍由现场业务流程产生。

SET NAMES utf8mb4;
START TRANSACTION;

-- ---------------------------------------------------------------------------
-- A. 统一组织、部门和演示账号
-- ---------------------------------------------------------------------------

INSERT INTO sys_organization
(organization_no, organization_name, organization_type, contact_name, address,
 status, is_deleted, created_at, updated_at, version, remark)
VALUES
('DEMO_HL_ORG', '石柱黄连数字化研究演示中心', 'research', '答辩演示组', '重庆市石柱县（演示地址）',
 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; demo_seed')
ON DUPLICATE KEY UPDATE
 organization_name=VALUES(organization_name), organization_type=VALUES(organization_type),
 contact_name=VALUES(contact_name), address=VALUES(address), status=1, is_deleted=0,
 updated_at=NOW(), remark=VALUES(remark);

SET @demo_org_id := (
  SELECT id FROM sys_organization
  WHERE organization_no='DEMO_HL_ORG' AND is_deleted=0 LIMIT 1
);

INSERT INTO sys_department
(department_no, department_name, organization_id, parent_id, sort_order,
 status, is_deleted, created_at, updated_at, version, remark)
VALUES
('DEMO_HL_DEPT', '黄连数字化研究室', @demo_org_id, 0, 10,
 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; demo_seed')
ON DUPLICATE KEY UPDATE
 department_name=VALUES(department_name), organization_id=VALUES(organization_id),
 parent_id=0, sort_order=VALUES(sort_order), status=1, is_deleted=0,
 updated_at=NOW(), remark=VALUES(remark);

SET @demo_dept_id := (
  SELECT id FROM sys_department
  WHERE department_no='DEMO_HL_DEPT' AND is_deleted=0 LIMIT 1
);

-- 兼容已执行过旧版演示脚本的本地库：将培训管理员原地替换为系统管理员。
-- 若管理员账号已存在，则停用旧账号，避免现场出现两个不在流程内的演示账号。
UPDATE sys_user legacy
LEFT JOIN sys_user admin_user
  ON admin_user.username='admin_demo' AND admin_user.is_deleted=0
SET legacy.user_no='DEMO_HL_ADMIN', legacy.username='admin_demo',
    legacy.real_name='黄连演示系统管理员', legacy.user_type='admin',
    legacy.must_change_password=0, legacy.status=1, legacy.is_deleted=0,
    legacy.updated_at=NOW(), legacy.remark='BDIS_DEFENSE_DEMO; admin; demo_seed'
WHERE legacy.username='trainer_hl_demo' AND legacy.is_deleted=0 AND admin_user.id IS NULL;

UPDATE sys_user legacy
JOIN sys_user admin_user
  ON admin_user.username='admin_demo' AND admin_user.is_deleted=0
SET legacy.status=0, legacy.is_deleted=1, legacy.updated_at=NOW(),
    legacy.remark='BDIS_DEFENSE_DEMO; replaced by admin_demo'
WHERE legacy.username='trainer_hl_demo' AND legacy.is_deleted=0;

-- 统一密码为 password，仅限本地演示库。
INSERT INTO sys_user
(user_no, username, password_hash, real_name, organization_id, department_id,
 user_type, must_change_password, status, is_deleted, created_at, updated_at, version, remark)
VALUES
('DEMO_AGENT_TEACHER', 'agent_teacher_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '黄连演示教师', @demo_org_id, @demo_dept_id, 'teacher', 0, 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; teacher; demo_seed'),
('DEMO_AGENT_COLLECTOR', 'collector_agent_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '黄连演示采集员', @demo_org_id, @demo_dept_id, 'collector', 0, 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; collector; demo_seed'),
('DEMO_HL_REVIEWER', 'agent_reviewer_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '黄连演示审核员', @demo_org_id, @demo_dept_id, 'reviewer', 0, 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; reviewer; demo_seed'),
('DEMO_HL_STUDENT', 'student_hl_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '黄连演示学生', @demo_org_id, @demo_dept_id, 'student', 0, 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; student; demo_seed'),
('DEMO_HL_RESEARCHER', 'researcher_hl_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '黄连演示研究员', @demo_org_id, @demo_dept_id, 'researcher', 0, 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; researcher; demo_seed'),
('DEMO_HL_ADMIN', 'admin_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '黄连演示系统管理员', @demo_org_id, @demo_dept_id, 'admin', 0, 1, 0, NOW(), NOW(), 0, 'BDIS_DEFENSE_DEMO; admin; demo_seed')
ON DUPLICATE KEY UPDATE
 real_name=VALUES(real_name), organization_id=VALUES(organization_id),
 department_id=VALUES(department_id), user_type=VALUES(user_type),
 must_change_password=0, status=1, is_deleted=0, updated_at=NOW(), remark=VALUES(remark);

SET @teacher_id := (SELECT id FROM sys_user WHERE username='agent_teacher_demo' AND is_deleted=0 LIMIT 1);
SET @collector_id := (SELECT id FROM sys_user WHERE username='collector_agent_demo' AND is_deleted=0 LIMIT 1);
SET @reviewer_id := (SELECT id FROM sys_user WHERE username='agent_reviewer_demo' AND is_deleted=0 LIMIT 1);
SET @student_id := (SELECT id FROM sys_user WHERE username='student_hl_demo' AND is_deleted=0 LIMIT 1);
SET @researcher_id := (SELECT id FROM sys_user WHERE username='researcher_hl_demo' AND is_deleted=0 LIMIT 1);
SET @admin_id := (SELECT id FROM sys_user WHERE username='admin_demo' AND is_deleted=0 LIMIT 1);

DELETE user_role
FROM rel_user_role user_role
JOIN sys_user user ON user.id=user_role.user_id
JOIN auth_role role ON role.id=user_role.role_id
WHERE user.username='admin_demo' AND role.role_code<>'ADMIN';

INSERT IGNORE INTO rel_user_role (user_id, role_id, created_at, created_by, remark)
SELECT u.id, r.id, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO; demo_seed'
FROM sys_user u
JOIN auth_role r ON r.role_code = CASE u.username
  WHEN 'agent_teacher_demo' THEN 'TEACHER'
  WHEN 'collector_agent_demo' THEN 'COLLECTOR'
  WHEN 'agent_reviewer_demo' THEN 'REVIEWER'
  WHEN 'student_hl_demo' THEN 'STUDENT'
  WHEN 'researcher_hl_demo' THEN 'RESEARCHER'
  WHEN 'admin_demo' THEN 'ADMIN'
END
WHERE u.username IN (
  'agent_teacher_demo', 'collector_agent_demo', 'agent_reviewer_demo',
  'student_hl_demo', 'researcher_hl_demo', 'admin_demo'
);

SET @species_id := (
  SELECT id FROM herb_species
  WHERE herb_no='DEMO_AGENT_HUANGLIAN' AND is_deleted=0 LIMIT 1
);
SET @base_id := (
  SELECT id FROM herb_base
  WHERE base_no='DEMO_AGENT_BASE' AND is_deleted=0 LIMIT 1
);
SET @task_id := (
  SELECT id FROM herb_collection_task
  WHERE task_code='DEMO_AGENT_HL_CONTINUOUS' AND is_deleted=0 LIMIT 1
);

UPDATE herb_species
SET herb_name='黄连', latin_name='Coptis chinensis Franch.', medicinal_part='根茎',
    growth_environment='温凉、湿润的林下环境（仅作演示文本）', origin_area='重庆石柱',
    description='石柱黄连全生命周期数字化研究演示药材；所有数据为演示样本。',
    status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id,
    remark='BDIS_DEFENSE_DEMO; demo_seed; not a field measurement'
WHERE id=@species_id;

UPDATE herb_base
SET base_name='石柱黄连科研 Agent 演示基地', base_type='planting',
    address='重庆市石柱县（演示地址）', longitude=108.2450000, latitude=30.1840000,
    contact_name='黄连演示采集员', description='仅用于 BDIS 答辩演示，非现场实测基地。',
    status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id,
    remark='BDIS_DEFENSE_DEMO; demo_seed; not a field measurement'
WHERE id=@base_id;

-- ---------------------------------------------------------------------------
-- B. 地图点位和适生分析样本
-- ---------------------------------------------------------------------------

INSERT INTO sys_file_resource
(file_no, file_name, original_filename, file_type, file_format, file_size,
 file_url, storage_path, storage_type, access_level, content_type,
 uploader_id, uploader_name, uploaded_at, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO_HL_MAP_FILE_01', 'map-point-01.png', '黄连主采集点封面.png', 'image', 'png', NULL, 'pending', 'defense-demo/map-point-01.png', 'local', 'public', 'image/png', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; map cover; demo_seed'),
('DEMO_HL_MAP_FILE_02', 'map-point-02.jpeg', '黄连林下观测点A封面.jpeg', 'image', 'jpeg', NULL, 'pending', 'defense-demo/map-point-02.jpeg', 'local', 'public', 'image/jpeg', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; map cover; demo_seed'),
('DEMO_HL_MAP_FILE_03', 'map-point-03.jpeg', '黄连坡地观测点B封面.jpeg', 'image', 'jpeg', NULL, 'pending', 'defense-demo/map-point-03.jpeg', 'local', 'public', 'image/jpeg', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; map cover; demo_seed'),
('DEMO_HL_MAP_FILE_04', 'map-point-04.jpeg', '黄连低海拔对照点C封面.jpeg', 'image', 'jpeg', NULL, 'pending', 'defense-demo/map-point-04.jpeg', 'local', 'public', 'image/jpeg', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; map cover; demo_seed'),
('DEMO_HL_MAP_FILE_05', 'map-point-05.png', '黄连林缘样地D封面.png', 'image', 'png', NULL, 'pending', 'defense-demo/map-point-05.png', 'local', 'public', 'image/png', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; map cover; demo_seed'),
('DEMO_HL_MAP_FILE_06', 'map-point-06.png', '黄连沟谷样地E封面.png', 'image', 'png', NULL, 'pending', 'defense-demo/map-point-06.png', 'local', 'public', 'image/png', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; map cover; demo_seed'),
('DEMO_HL_MAP_FILE_07', 'map-point-07.jpeg', '黄连高海拔对照点F封面.jpeg', 'image', 'jpeg', NULL, 'pending', 'defense-demo/map-point-07.jpeg', 'local', 'public', 'image/jpeg', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; map cover; demo_seed'),
('DEMO_HL_SPECIES_COVER', 'huanglian-species-cover.png', '黄连药材品种封面.png', 'image', 'png', NULL, 'pending', 'defense-demo/huanglian-species-cover.png', 'local', 'public', 'image/png', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; herb species cover; demo_seed'),
('DEMO_HERB_COVER_DANGSHEN', 'herb-cover-dangshen.jpeg', '党参药材品种封面.jpeg', 'image', 'jpeg', NULL, 'pending', 'defense-demo/herb-cover-dangshen.jpeg', 'local', 'public', 'image/jpeg', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; standard herb species cover; demo_seed'),
('DEMO_HERB_COVER_GOUQI', 'herb-cover-gouqi.jpeg', '枸杞药材品种封面.jpeg', 'image', 'jpeg', NULL, 'pending', 'defense-demo/herb-cover-gouqi.jpeg', 'local', 'public', 'image/jpeg', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; standard herb species cover; demo_seed'),
('DEMO_HERB_COVER_HUANGLIAN', 'herb-cover-huanglian.png', '黄连标准药材品种封面.png', 'image', 'png', NULL, 'pending', 'defense-demo/herb-cover-huanglian.png', 'local', 'public', 'image/png', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; standard herb species cover; demo_seed'),
('DEMO_HERB_COVER_HUANGQI', 'herb-cover-huangqi.png', '黄芪药材品种封面.png', 'image', 'png', NULL, 'pending', 'defense-demo/herb-cover-huangqi.png', 'local', 'public', 'image/png', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; standard herb species cover; demo_seed'),
('DEMO_HERB_COVER_JINYINHUA', 'herb-cover-jinyinhua.jpeg', '金银花药材品种封面.jpeg', 'image', 'jpeg', NULL, 'pending', 'defense-demo/herb-cover-jinyinhua.jpeg', 'local', 'public', 'image/jpeg', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; standard herb species cover; demo_seed')
ON DUPLICATE KEY UPDATE
 file_name=VALUES(file_name), original_filename=VALUES(original_filename), file_type=VALUES(file_type),
 file_format=VALUES(file_format), storage_path=VALUES(storage_path), storage_type='local',
 access_level='public', content_type=VALUES(content_type), uploader_id=VALUES(uploader_id),
 uploader_name=VALUES(uploader_name), status=1, is_deleted=0, updated_at=NOW(),
 updated_by=@teacher_id, remark=VALUES(remark);

UPDATE sys_file_resource
SET file_url=CONCAT('/api/public-files/', id, '/content')
WHERE file_no IN ('DEMO_HL_MAP_FILE_01','DEMO_HL_MAP_FILE_02','DEMO_HL_MAP_FILE_03','DEMO_HL_MAP_FILE_04','DEMO_HL_MAP_FILE_05','DEMO_HL_MAP_FILE_06','DEMO_HL_MAP_FILE_07','DEMO_HL_SPECIES_COVER','DEMO_HERB_COVER_DANGSHEN','DEMO_HERB_COVER_GOUQI','DEMO_HERB_COVER_HUANGLIAN','DEMO_HERB_COVER_HUANGQI','DEMO_HERB_COVER_JINYINHUA');

SET @map_file_01 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_MAP_FILE_01' LIMIT 1);
SET @map_file_02 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_MAP_FILE_02' LIMIT 1);
SET @map_file_03 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_MAP_FILE_03' LIMIT 1);
SET @map_file_04 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_MAP_FILE_04' LIMIT 1);
SET @map_file_05 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_MAP_FILE_05' LIMIT 1);
SET @map_file_06 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_MAP_FILE_06' LIMIT 1);
SET @map_file_07 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_MAP_FILE_07' LIMIT 1);
SET @species_cover_file := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_SPECIES_COVER' LIMIT 1);

SET @point_01 := (SELECT id FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M01%' AND is_deleted=0 ORDER BY id LIMIT 1);
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @species_id, @base_id, '石柱黄连科研演示基地', 108.2450000, 30.1840000,
 '重庆市', '重庆市', '石柱土家族自治县', '黄连主采集点（演示）',
 1480.00, 'artificial', 'core', '主采集点；非现场实测坐标。', CONCAT('/api/public-files/',@map_file_01,'/content'),
 '2026-07-05 09:20:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:M01; DEMO_HL_POINT_MAIN; not field-measured'
WHERE @point_01 IS NULL;
SET @point_01 := COALESCE(@point_01, LAST_INSERT_ID());

SET @point_02 := (SELECT id FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M02%' AND is_deleted=0 ORDER BY id LIMIT 1);
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @species_id, @base_id, '黄连林下观测点 A（演示）', 108.2860000, 30.2050000,
 '重庆市', '重庆市', '石柱土家族自治县', '林下观测点 A（演示）',
 1520.00, 'artificial', 'secondary', '高海拔林下观测演示点。', CONCAT('/api/public-files/',@map_file_02,'/content'),
 '2026-07-09 09:30:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:M02; DEMO_HL_POINT_A; not field-measured'
WHERE @point_02 IS NULL;
SET @point_02 := COALESCE(@point_02, LAST_INSERT_ID());

SET @point_03 := (SELECT id FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M03%' AND is_deleted=0 ORDER BY id LIMIT 1);
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @species_id, @base_id, '黄连坡地观测点 B（演示）', 108.3310000, 30.1600000,
 '重庆市', '重庆市', '石柱土家族自治县', '坡地观测点 B（演示）',
 1560.00, 'artificial', 'secondary', '坡地开花期观测演示点。', CONCAT('/api/public-files/',@map_file_03,'/content'),
 '2026-07-08 09:30:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:M03; DEMO_HL_POINT_B; not field-measured'
WHERE @point_03 IS NULL;
SET @point_03 := COALESCE(@point_03, LAST_INSERT_ID());

SET @point_04 := (SELECT id FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M04%' AND is_deleted=0 ORDER BY id LIMIT 1);
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @species_id, @base_id, '黄连低海拔对照点 C（演示）', 108.0200000, 30.4200000,
 '重庆市', '重庆市', '石柱土家族自治县', '低海拔对照点 C（演示）',
 650.00, 'artificial', 'comparison', '低海拔演示对照；不构成农学结论。', CONCAT('/api/public-files/',@map_file_04,'/content'),
 '2026-07-07 09:30:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:M04; DEMO_HL_POINT_C; not field-measured'
WHERE @point_04 IS NULL;
SET @point_04 := COALESCE(@point_04, LAST_INSERT_ID());

SET @point_05 := (SELECT id FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M05%' AND is_deleted=0 ORDER BY id LIMIT 1);
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @species_id, @base_id, '黄连林缘样地 D（演示）', 108.3100000, 30.2250000,
 '重庆市', '重庆市', '石柱土家族自治县', '林缘样地 D（演示）',
 1450.00, 'artificial', 'secondary', '林缘遮阴环境观测演示点。', CONCAT('/api/public-files/',@map_file_05,'/content'),
 '2026-07-10 09:40:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:M05; DEMO_HL_POINT_D; not field-measured'
WHERE @point_05 IS NULL;
SET @point_05 := COALESCE(@point_05, LAST_INSERT_ID());

SET @point_06 := (SELECT id FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M06%' AND is_deleted=0 ORDER BY id LIMIT 1);
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @species_id, @base_id, '黄连沟谷样地 E（演示）', 108.2700000, 30.1450000,
 '重庆市', '重庆市', '石柱土家族自治县', '沟谷样地 E（演示）',
 1320.00, 'artificial', 'secondary', '沟谷湿润环境观测演示点。', CONCAT('/api/public-files/',@map_file_06,'/content'),
 '2026-07-11 09:10:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:M06; DEMO_HL_POINT_E; not field-measured'
WHERE @point_06 IS NULL;
SET @point_06 := COALESCE(@point_06, LAST_INSERT_ID());

SET @point_07 := (SELECT id FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:M07%' AND is_deleted=0 ORDER BY id LIMIT 1);
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @species_id, @base_id, '黄连高海拔对照点 F（演示）', 108.3700000, 30.2400000,
 '重庆市', '重庆市', '石柱土家族自治县', '高海拔对照点 F（演示）',
 1680.00, 'artificial', 'comparison', '高海拔阴湿环境演示对照点。', CONCAT('/api/public-files/',@map_file_07,'/content'),
 '2026-07-12 09:50:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:M07; DEMO_HL_POINT_F; not field-measured'
WHERE @point_07 IS NULL;
SET @point_07 := COALESCE(@point_07, LAST_INSERT_ID());

UPDATE herb_distribution SET
 species_id=@species_id, base_id=@base_id, location_name='石柱黄连科研演示基地',
 longitude=108.2450000, latitude=30.1840000, altitude=1480.00,
 cover_image_url=CONCAT('/api/public-files/',@map_file_01,'/content'), last_collected_at='2026-07-05 09:20:00',
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@point_01;
UPDATE herb_distribution SET
 species_id=@species_id, base_id=@base_id, location_name='黄连林下观测点 A（演示）',
 longitude=108.2860000, latitude=30.2050000, altitude=1520.00,
 cover_image_url=CONCAT('/api/public-files/',@map_file_02,'/content'), last_collected_at='2026-07-09 09:30:00',
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@point_02;
UPDATE herb_distribution SET
 species_id=@species_id, base_id=@base_id, location_name='黄连坡地观测点 B（演示）',
 longitude=108.3310000, latitude=30.1600000, altitude=1560.00,
 cover_image_url=CONCAT('/api/public-files/',@map_file_03,'/content'), last_collected_at='2026-07-08 09:30:00',
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@point_03;
UPDATE herb_distribution SET
 species_id=@species_id, base_id=@base_id, location_name='黄连低海拔对照点 C（演示）',
 longitude=108.0200000, latitude=30.4200000, altitude=650.00,
 cover_image_url=CONCAT('/api/public-files/',@map_file_04,'/content'), last_collected_at='2026-07-07 09:30:00',
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@point_04;
UPDATE herb_distribution SET
 species_id=@species_id, base_id=@base_id, location_name='黄连林缘样地 D（演示）',
 longitude=108.3100000, latitude=30.2250000, altitude=1450.00,
 cover_image_url=CONCAT('/api/public-files/',@map_file_05,'/content'), last_collected_at='2026-07-10 09:40:00',
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@point_05;
UPDATE herb_distribution SET
 species_id=@species_id, base_id=@base_id, location_name='黄连沟谷样地 E（演示）',
 longitude=108.2700000, latitude=30.1450000, altitude=1320.00,
 cover_image_url=CONCAT('/api/public-files/',@map_file_06,'/content'), last_collected_at='2026-07-11 09:10:00',
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@point_06;
UPDATE herb_distribution SET
 species_id=@species_id, base_id=@base_id, location_name='黄连高海拔对照点 F（演示）',
 longitude=108.3700000, latitude=30.2400000, altitude=1680.00,
 cover_image_url=CONCAT('/api/public-files/',@map_file_07,'/content'), last_collected_at='2026-07-12 09:50:00',
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@point_07;

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@map_file_01, 'map_point', @point_01, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO'),
(@map_file_02, 'map_point', @point_02, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO'),
(@map_file_03, 'map_point', @point_03, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO'),
(@map_file_04, 'map_point', @point_04, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO'),
(@map_file_05, 'map_point', @point_05, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO'),
(@map_file_06, 'map_point', @point_06, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO'),
(@map_file_07, 'map_point', @point_07, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO');

UPDATE herb_species
SET cover_image_url=CONCAT('/api/public-files/',@species_cover_file,'/content'),
    updated_at=NOW(), updated_by=@teacher_id
WHERE id=@species_id;

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@species_cover_file, 'herb_species', @species_id, 'cover', 1, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO; herb species cover; demo_seed');

UPDATE herb_species species
JOIN sys_file_resource file_resource ON file_resource.file_no=CASE species.herb_no
  WHEN 'HERB_DANGSHEN' THEN 'DEMO_HERB_COVER_DANGSHEN'
  WHEN 'HERB_GOUQI' THEN 'DEMO_HERB_COVER_GOUQI'
  WHEN 'HERB_HUANGLIAN' THEN 'DEMO_HERB_COVER_HUANGLIAN'
  WHEN 'HERB_HUANGQI' THEN 'DEMO_HERB_COVER_HUANGQI'
  WHEN 'HERB_JINYINHUA' THEN 'DEMO_HERB_COVER_JINYINHUA'
END
SET species.cover_image_url=CONCAT('/api/public-files/', file_resource.id, '/content'),
    species.status=1, species.is_deleted=0, species.updated_at=NOW(), species.updated_by=@teacher_id
WHERE species.herb_no IN ('HERB_DANGSHEN','HERB_GOUQI','HERB_HUANGLIAN','HERB_HUANGQI','HERB_JINYINHUA')
  AND file_resource.is_deleted=0;

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
SELECT file_resource.id, 'herb_species', species.id, 'cover', 1, 1, NOW(), @teacher_id,
       'BDIS_DEFENSE_DEMO; standard herb species cover; demo_seed'
FROM herb_species species
JOIN sys_file_resource file_resource ON file_resource.file_no=CASE species.herb_no
  WHEN 'HERB_DANGSHEN' THEN 'DEMO_HERB_COVER_DANGSHEN'
  WHEN 'HERB_GOUQI' THEN 'DEMO_HERB_COVER_GOUQI'
  WHEN 'HERB_HUANGLIAN' THEN 'DEMO_HERB_COVER_HUANGLIAN'
  WHEN 'HERB_HUANGQI' THEN 'DEMO_HERB_COVER_HUANGQI'
  WHEN 'HERB_JINYINHUA' THEN 'DEMO_HERB_COVER_JINYINHUA'
END
WHERE species.herb_no IN ('HERB_DANGSHEN','HERB_GOUQI','HERB_HUANGLIAN','HERB_HUANGQI','HERB_JINYINHUA')
  AND species.is_deleted=0 AND file_resource.is_deleted=0;

-- 地图背景补充：其他药材仅提供稳定的采集地点，不关联黄连主流程的生长记录、批次或审核链路。
INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT species.id, NULL, '城口党参示范采集点（演示）', 108.6600000, 32.0300000,
 '重庆市', '重庆市', '城口县', '城口县北屏乡党参示范采集点（演示）',
 1350.00, 'cultivated', 'secondary', '地图背景采集点；仅作答辩演示，不进入黄连主流程。', species.cover_image_url,
 '2026-07-03 10:00:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:O01; DEMO_OTHER_DANGSHEN; not field-measured'
FROM herb_species species
WHERE species.herb_no='HERB_DANGSHEN' AND species.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:O01%' AND is_deleted=0);

INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT species.id, NULL, '江津枸杞标准化采集点（演示）', 106.4100000, 28.6200000,
 '重庆市', '重庆市', '江津区', '江津区四面山镇枸杞标准化采集点（演示）',
 780.00, 'cultivated', 'secondary', '地图背景采集点；仅作答辩演示，不进入黄连主流程。', species.cover_image_url,
 '2026-07-04 10:00:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:O02; DEMO_OTHER_GOUQI; not field-measured'
FROM herb_species species
WHERE species.herb_no='HERB_GOUQI' AND species.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:O02%' AND is_deleted=0);

INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT species.id, NULL, '巫溪黄芪生态采集点（演示）', 109.1500000, 31.4900000,
 '重庆市', '重庆市', '巫溪县', '巫溪县红池坝镇黄芪生态采集点（演示）',
 1650.00, 'cultivated', 'secondary', '地图背景采集点；仅作答辩演示，不进入黄连主流程。', species.cover_image_url,
 '2026-07-05 10:00:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:O03; DEMO_OTHER_HUANGQI; not field-measured'
FROM herb_species species
WHERE species.herb_no='HERB_HUANGQI' AND species.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:O03%' AND is_deleted=0);

INSERT INTO herb_distribution
(species_id, base_id, location_name, longitude, latitude, province, city, district, address,
 altitude, distribution_type, distribution_level, distribution_desc, cover_image_url,
 last_collected_at, source_type, data_source, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT species.id, NULL, '秀山金银花种植采集点（演示）', 109.0200000, 28.3800000,
 '重庆市', '重庆市', '秀山土家族苗族自治县', '秀山县清溪场街道金银花种植采集点（演示）',
 420.00, 'cultivated', 'secondary', '地图背景采集点；仅作答辩演示，不进入黄连主流程。', species.cover_image_url,
 '2026-07-06 10:00:00', 'pc', 'demo_seed', 1, 0,
 NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:O04; DEMO_OTHER_JINYINHUA; not field-measured'
FROM herb_species species
WHERE species.herb_no='HERB_JINYINHUA' AND species.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM herb_distribution WHERE remark LIKE 'BDIS_DEFENSE_DEMO:O04%' AND is_deleted=0);

-- 重复执行演示数据脚本时，同步迁移已经存在的其他药材点位。
UPDATE herb_distribution point
JOIN herb_species species ON species.herb_no='HERB_DANGSHEN' AND species.is_deleted=0
SET point.species_id=species.id, point.base_id=NULL,
    point.location_name='城口党参示范采集点（演示）',
    point.longitude=108.6600000, point.latitude=32.0300000,
    point.province='重庆市', point.city='重庆市', point.district='城口县',
    point.address='城口县北屏乡党参示范采集点（演示）', point.altitude=1350.00,
    point.cover_image_url=species.cover_image_url, point.status=1,
    point.updated_at=NOW(), point.updated_by=@teacher_id
WHERE point.remark LIKE 'BDIS_DEFENSE_DEMO:O01%' AND point.is_deleted=0;

UPDATE herb_distribution point
JOIN herb_species species ON species.herb_no='HERB_GOUQI' AND species.is_deleted=0
SET point.species_id=species.id, point.base_id=NULL,
    point.location_name='江津枸杞标准化采集点（演示）',
    point.longitude=106.4100000, point.latitude=28.6200000,
    point.province='重庆市', point.city='重庆市', point.district='江津区',
    point.address='江津区四面山镇枸杞标准化采集点（演示）', point.altitude=780.00,
    point.cover_image_url=species.cover_image_url, point.status=1,
    point.updated_at=NOW(), point.updated_by=@teacher_id
WHERE point.remark LIKE 'BDIS_DEFENSE_DEMO:O02%' AND point.is_deleted=0;

UPDATE herb_distribution point
JOIN herb_species species ON species.herb_no='HERB_HUANGQI' AND species.is_deleted=0
SET point.species_id=species.id, point.base_id=NULL,
    point.location_name='巫溪黄芪生态采集点（演示）',
    point.longitude=109.1500000, point.latitude=31.4900000,
    point.province='重庆市', point.city='重庆市', point.district='巫溪县',
    point.address='巫溪县红池坝镇黄芪生态采集点（演示）', point.altitude=1650.00,
    point.cover_image_url=species.cover_image_url, point.status=1,
    point.updated_at=NOW(), point.updated_by=@teacher_id
WHERE point.remark LIKE 'BDIS_DEFENSE_DEMO:O03%' AND point.is_deleted=0;

UPDATE herb_distribution point
JOIN herb_species species ON species.herb_no='HERB_JINYINHUA' AND species.is_deleted=0
SET point.species_id=species.id, point.base_id=NULL,
    point.location_name='秀山金银花种植采集点（演示）',
    point.longitude=109.0200000, point.latitude=28.3800000,
    point.province='重庆市', point.city='重庆市', point.district='秀山土家族苗族自治县',
    point.address='秀山县清溪场街道金银花种植采集点（演示）', point.altitude=420.00,
    point.cover_image_url=species.cover_image_url, point.status=1,
    point.updated_at=NOW(), point.updated_by=@teacher_id
WHERE point.remark LIKE 'BDIS_DEFENSE_DEMO:O04%' AND point.is_deleted=0;

-- 原任务三阶段全部归属主点，适生分析取最新的旺盛生长期记录。
UPDATE herb_growth_record
SET distribution_id=@point_01, updated_at=NOW(), updated_by=@reviewer_id
WHERE task_id=@task_id AND is_deleted=0;

INSERT INTO herb_growth_record
(species_id, species_name, base_id, base_name, distribution_id,
 collector_id, collector_name_snapshot, longitude, latitude, growth_stage,
 plant_height, stem_diameter, soil_ph, temperature, humidity, soil_moisture, light,
 leaf_color, flowering_status, growth_evaluation, device_type, data_source,
 external_source, external_no, review_status, submitted_at, reviewed_at, collected_at,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
(@species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', @point_02,
 @collector_id, '黄连演示采集员', 108.2860000, 30.2050000, '成熟期',
 18.60, 3.60, 6.18, 22.80, 70.00, 41.00, 13600,
 '深绿', '已开花', '演示成熟期记录。', 'app', 'demo_seed',
 'defense_demo', 'DEMO-HL-MAP-02', 'approved', '2026-07-09 10:00:00', '2026-07-09 15:00:00', '2026-07-09 09:30:00',
 1, 0, NOW(), NOW(), @collector_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:M02; not field-measured'),
(@species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', @point_03,
 @collector_id, '黄连演示采集员', 108.3310000, 30.1600000, '开花期',
 16.80, 3.30, 6.08, 23.10, 67.00, 36.00, 14500,
 '绿色', '开花', '演示开花期记录。', 'app', 'demo_seed',
 'defense_demo', 'DEMO-HL-MAP-03', 'approved', '2026-07-08 10:00:00', '2026-07-08 15:00:00', '2026-07-08 09:30:00',
 1, 0, NOW(), NOW(), @collector_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:M03; not field-measured'),
(@species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', @point_04,
 @collector_id, '黄连演示采集员', 108.0200000, 30.4200000, '幼苗期',
 6.20, 1.50, 6.35, 26.40, 61.00, 28.00, 15800,
 '嫩绿', '未开花', '低海拔演示对照，不构成农学结论。', 'app', 'demo_seed',
 'defense_demo', 'DEMO-HL-MAP-04', 'approved', '2026-07-07 10:00:00', '2026-07-07 15:00:00', '2026-07-07 09:30:00',
 1, 0, NOW(), NOW(), @collector_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:M04; not field-measured'),
(@species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', @point_05,
 @collector_id, '黄连演示采集员', 108.3100000, 30.2250000, '展叶期',
 14.10, 3.00, 6.12, 21.60, 74.00, 48.00, 11800,
 '绿色', '未开花', '林缘遮阴样地演示记录。', 'app', 'demo_seed',
 'defense_demo', 'DEMO-HL-MAP-05', 'approved', '2026-07-10 10:10:00', '2026-07-10 15:10:00', '2026-07-10 09:40:00',
 1, 0, NOW(), NOW(), @collector_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:M05; not field-measured'),
(@species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', @point_06,
 @collector_id, '黄连演示采集员', 108.2700000, 30.1450000, '成熟期',
 19.40, 3.80, 6.06, 23.40, 78.00, 53.00, 10900,
 '深绿', '已开花', '沟谷湿润样地演示记录。', 'app', 'demo_seed',
 'defense_demo', 'DEMO-HL-MAP-06', 'approved', '2026-07-11 10:00:00', '2026-07-11 15:00:00', '2026-07-11 09:10:00',
 1, 0, NOW(), NOW(), @collector_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:M06; not field-measured'),
(@species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', @point_07,
 @collector_id, '黄连演示采集员', 108.3700000, 30.2400000, '花期',
 17.30, 3.50, 6.22, 18.80, 81.00, 58.00, 9800,
 '深绿', '开花', '高海拔阴湿对照点演示记录。', 'app', 'demo_seed',
 'defense_demo', 'DEMO-HL-MAP-07', 'approved', '2026-07-12 10:20:00', '2026-07-12 15:20:00', '2026-07-12 09:50:00',
 1, 0, NOW(), NOW(), @collector_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:M07; not field-measured')
ON DUPLICATE KEY UPDATE
 species_id=VALUES(species_id), base_id=VALUES(base_id), distribution_id=VALUES(distribution_id),
 collector_id=VALUES(collector_id), collector_name_snapshot=VALUES(collector_name_snapshot),
 longitude=VALUES(longitude), latitude=VALUES(latitude), growth_stage=VALUES(growth_stage),
 plant_height=VALUES(plant_height), stem_diameter=VALUES(stem_diameter), soil_ph=VALUES(soil_ph),
 temperature=VALUES(temperature), humidity=VALUES(humidity), soil_moisture=VALUES(soil_moisture),
 light=VALUES(light), review_status='approved', submitted_at=VALUES(submitted_at),
 reviewed_at=VALUES(reviewed_at), collected_at=VALUES(collected_at), status=1, is_deleted=0,
 updated_at=NOW(), updated_by=@reviewer_id, remark=VALUES(remark);

-- ---------------------------------------------------------------------------
-- C. 原任务三阶段审核留痕
-- ---------------------------------------------------------------------------

SET @batch_01 := (SELECT id FROM herb_batch WHERE batch_code='DEMO_AGENT_HL_STAGE_01' LIMIT 1);
SET @batch_02 := (SELECT id FROM herb_batch WHERE batch_code='DEMO_AGENT_HL_STAGE_02' LIMIT 1);
SET @batch_03 := (SELECT id FROM herb_batch WHERE batch_code='DEMO_AGENT_HL_STAGE_03' LIMIT 1);
SET @record_01 := (SELECT id FROM herb_growth_record WHERE batch_id=@batch_01 LIMIT 1);
SET @record_02 := (SELECT id FROM herb_growth_record WHERE batch_id=@batch_02 LIMIT 1);
SET @record_03 := (SELECT id FROM herb_growth_record WHERE batch_id=@batch_03 LIMIT 1);

UPDATE herb_growth_record
SET review_status='approved', updated_by=@reviewer_id,
    reviewed_at=CASE id
      WHEN @record_01 THEN '2026-06-02 09:00:00'
      WHEN @record_02 THEN '2026-06-19 09:00:00'
      WHEN @record_03 THEN '2026-07-06 09:00:00'
      ELSE reviewed_at END,
    distribution_id=@point_01, updated_at=NOW()
WHERE id IN (@record_01,@record_02,@record_03);

INSERT INTO herb_growth_review_record
(growth_record_id, reviewer_id, reviewer_name, reviewer_role, review_action,
 before_status, after_status, review_comment, reviewed_at, created_at, created_by, remark)
SELECT @record_01, @collector_id, '黄连演示采集员', 'COLLECTOR', 'submit',
 'draft', 'submitted', '阶段一采集记录已提交审核。', '2026-06-01 10:10:00', '2026-06-01 10:10:00', @collector_id, 'BDIS_DEFENSE_DEMO:P02:submit'
WHERE @record_01 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM herb_growth_review_record WHERE growth_record_id=@record_01 AND remark='BDIS_DEFENSE_DEMO:P02:submit'
);
INSERT INTO herb_growth_review_record
(growth_record_id, reviewer_id, reviewer_name, reviewer_role, review_action,
 before_status, after_status, review_comment, reviewed_at, created_at, created_by, remark)
SELECT @record_01, @reviewer_id, '黄连演示审核员', 'REVIEWER', 'approve',
 'submitted', 'approved', '指标、时间和整株影像完整，审核通过。', '2026-06-02 09:00:00', '2026-06-02 09:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:P02:approve'
WHERE @record_01 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM herb_growth_review_record WHERE growth_record_id=@record_01 AND remark='BDIS_DEFENSE_DEMO:P02:approve'
);

INSERT INTO herb_growth_review_record
(growth_record_id, reviewer_id, reviewer_name, reviewer_role, review_action,
 before_status, after_status, review_comment, reviewed_at, created_at, created_by, remark)
SELECT @record_02, @collector_id, '黄连演示采集员', 'COLLECTOR', 'submit',
 'draft', 'submitted', '阶段二采集记录已提交审核。', '2026-06-18 10:10:00', '2026-06-18 10:10:00', @collector_id, 'BDIS_DEFENSE_DEMO:P03:submit'
WHERE @record_02 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM herb_growth_review_record WHERE growth_record_id=@record_02 AND remark='BDIS_DEFENSE_DEMO:P03:submit'
);
INSERT INTO herb_growth_review_record
(growth_record_id, reviewer_id, reviewer_name, reviewer_role, review_action,
 before_status, after_status, review_comment, reviewed_at, created_at, created_by, remark)
SELECT @record_02, @reviewer_id, '黄连演示审核员', 'REVIEWER', 'approve',
 'submitted', 'approved', '叶片影像与生长指标可用，审核通过。', '2026-06-19 09:00:00', '2026-06-19 09:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:P03:approve'
WHERE @record_02 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM herb_growth_review_record WHERE growth_record_id=@record_02 AND remark='BDIS_DEFENSE_DEMO:P03:approve'
);

INSERT INTO herb_growth_review_record
(growth_record_id, reviewer_id, reviewer_name, reviewer_role, review_action,
 before_status, after_status, review_comment, reviewed_at, created_at, created_by, remark)
SELECT @record_03, @collector_id, '黄连演示采集员', 'COLLECTOR', 'submit',
 'draft', 'submitted', '阶段三采集记录已提交审核。', '2026-07-05 10:10:00', '2026-07-05 10:10:00', @collector_id, 'BDIS_DEFENSE_DEMO:P04:submit'
WHERE @record_03 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM herb_growth_review_record WHERE growth_record_id=@record_03 AND remark='BDIS_DEFENSE_DEMO:P04:submit'
);
INSERT INTO herb_growth_review_record
(growth_record_id, reviewer_id, reviewer_name, reviewer_role, review_action,
 before_status, after_status, review_comment, reviewed_at, created_at, created_by, remark)
SELECT @record_03, @reviewer_id, '黄连演示审核员', 'REVIEWER', 'approve',
 'submitted', 'approved', '业务记录可用；土壤 pH 缺失、根茎图缺失和识别不确定性由科研 Agent 继续分析。',
 '2026-07-06 09:00:00', '2026-07-06 09:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:P04:approve'
WHERE @record_03 IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM herb_growth_review_record WHERE growth_record_id=@record_03 AND remark='BDIS_DEFENSE_DEMO:P04:approve'
);

-- ---------------------------------------------------------------------------
-- D. 普通业务：课程、课题和培训
-- ---------------------------------------------------------------------------

INSERT INTO edu_course
(course_no, course_name, course_type, teacher_id, description, publish_status,
 started_at, ended_at, published_at, published_by, applicable_majors, hours, credits,
 prerequisites, teaching_objectives, teaching_methods, tags,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-COURSE-01', '黄连生长观测与数字化鉴别实验', 'experiment', @teacher_id,
 '以黄连为主线，练习地图选点、生长指标记录、形态影像采集、AI 识别和数字档案核验。', 'published',
 '2026-03-01 08:00:00', '2026-07-10 18:00:00', '2026-02-20 09:00:00', @teacher_id,
 '中药学、生物制药、数字农业', 32, 2.00,
 '掌握中药材基础鉴别知识。',
 '能够完成黄连生长观测、影像证据采集与数字档案核验。',
 '讲授、示范、实验和数字化工具实践。', '黄连,野外采集,图像识别,数字档案',
 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S01; demo_seed'),
('DEMO-HL-COURSE-02', '黄连规范化采集与质量控制实训', 'experiment', @teacher_id,
 '围绕样方选择、环境指标采集、规范拍摄和批次提交审核开展的已发布实训课程。', 'published',
 '2026-04-06 08:00:00', '2026-05-22 18:00:00', '2026-03-25 09:00:00', @teacher_id,
 '中药学、生物制药、药学', 24, 1.50,
 '完成中药材基础鉴别课程。',
 '能够按采集规范完成样方记录、影像采集和质量自检。',
 '讲授、实训、案例复盘。', '黄连,采集规范,质量控制,批次审核',
 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; published course; demo_seed'),
('DEMO-HL-COURSE-03', '中药材数字档案与溯源应用', 'mixed', @teacher_id,
 '面向已完成采集的药材记录，学习二维码公开页、证据链和数字档案核验的混合式课程。', 'published',
 '2026-05-11 08:00:00', '2026-06-26 18:00:00', '2026-04-28 09:00:00', @teacher_id,
 '中药学、信息管理、数字农业', 20, 1.00,
 '了解中药材采集与档案基础。',
 '能够说明公开溯源页、二维码和证据链之间的关联。',
 '在线学习、案例讨论、档案核验练习。', '数字档案,二维码溯源,证据链,黄连',
 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; published course; demo_seed')
ON DUPLICATE KEY UPDATE
 course_name=VALUES(course_name), course_type=VALUES(course_type), teacher_id=VALUES(teacher_id),
 description=VALUES(description), publish_status='published', started_at=VALUES(started_at),
 ended_at=VALUES(ended_at), published_at=VALUES(published_at), published_by=VALUES(published_by),
 applicable_majors=VALUES(applicable_majors), hours=VALUES(hours), credits=VALUES(credits),
 prerequisites=VALUES(prerequisites), teaching_objectives=VALUES(teaching_objectives),
 teaching_methods=VALUES(teaching_methods), tags=VALUES(tags), status=1, is_deleted=0,
 updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

SET @course_id := (SELECT id FROM edu_course WHERE course_no='DEMO-HL-COURSE-01' AND is_deleted=0 LIMIT 1);
SET @course_02_id := (SELECT id FROM edu_course WHERE course_no='DEMO-HL-COURSE-02' AND is_deleted=0 LIMIT 1);
SET @course_03_id := (SELECT id FROM edu_course WHERE course_no='DEMO-HL-COURSE-03' AND is_deleted=0 LIMIT 1);

INSERT INTO edu_experiment_step
(course_id, step_no, step_title, step_content, expected_result, sort_order,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
(@course_id, '01', '点位与样本准备', '在地图中查看黄连点位、海拔和生长阶段，选定观测点并准备记录项。', '确认点位、药材、基地和待采集指标。', 1, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S02'),
(@course_id, '02', '形态观察与影像采集', '观察黄连整株、叶片和根茎形态，按采集规范记录生长指标并拍摄影像。', '形成指标完整、类型明确的影像证据。', 2, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S02'),
(@course_id, '03', 'AI 识别与档案核验', '查看 AI 识别候选和置信度，对照审核留痕、数字档案阶段和证据链校验结果。', '能够解释识别不确定性与可追溯档案的关系。', 3, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S02')
ON DUPLICATE KEY UPDATE
 step_title=VALUES(step_title), step_content=VALUES(step_content), expected_result=VALUES(expected_result),
 sort_order=VALUES(sort_order), status=1, is_deleted=0, updated_at=NOW(),
 updated_by=@teacher_id, remark=VALUES(remark);

INSERT INTO rel_course_species
(course_id, species_id, relation_type, is_required, sort_order,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
SELECT @course_id, @species_id, 'material', 1, 1,
 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S01'
WHERE NOT EXISTS (
  SELECT 1 FROM rel_course_species
  WHERE course_id=@course_id AND species_id=@species_id AND is_deleted=0
);

INSERT INTO edu_experiment_record
(record_no, course_id, project_id, experiment_title, experiment_process, experiment_result,
 recorder_id, recorded_at, archive_status, submitted_at, submitted_by,
 score, graded_by, graded_at, grade_comment, report_file_id,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-EXP-01', @course_id, NULL, '黄连三阶段形态观察实验报告',
 '对返青期、展叶期和旺盛生长期的指标与影像进行对比，记录样本差异和识别结果。',
 '三阶段株高递增，土壤湿度下降；第三阶段存在 pH 和根茎影像证据缺口。',
 @student_id, '2026-07-08 14:00:00', 'submitted', '2026-07-08 15:00:00', @student_id,
 92.00, @teacher_id, '2026-07-09 10:00:00', '记录完整，能够结合影像说明阶段差异。', NULL,
 1, 0, NOW(), NOW(), @student_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S04; demo_seed')
ON DUPLICATE KEY UPDATE
 course_id=VALUES(course_id), experiment_title=VALUES(experiment_title),
 experiment_process=VALUES(experiment_process), experiment_result=VALUES(experiment_result),
 recorder_id=VALUES(recorder_id), recorded_at=VALUES(recorded_at), archive_status='submitted',
 submitted_at=VALUES(submitted_at), submitted_by=VALUES(submitted_by), score=VALUES(score),
 graded_by=VALUES(graded_by), graded_at=VALUES(graded_at), grade_comment=VALUES(grade_comment),
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

SET @experiment_id := (SELECT id FROM edu_experiment_record WHERE record_no='DEMO-HL-EXP-01' AND is_deleted=0 LIMIT 1);

INSERT INTO edu_experiment_record_review
(record_id, version_id, action, comment, score, operator_id, operated_at, version)
SELECT @experiment_id, NULL, 'submit', '学生提交黄连三阶段实验报告。', NULL, @student_id, '2026-07-08 15:00:00', 0
WHERE NOT EXISTS (
  SELECT 1 FROM edu_experiment_record_review WHERE record_id=@experiment_id AND action='submit'
);
INSERT INTO edu_experiment_record_review
(record_id, version_id, action, comment, score, operator_id, operated_at, version)
SELECT @experiment_id, NULL, 'grade', '记录完整，能够结合影像说明阶段差异。', 92.00, @teacher_id, '2026-07-09 10:00:00', 0
WHERE NOT EXISTS (
  SELECT 1 FROM edu_experiment_record_review WHERE record_id=@experiment_id AND action='grade'
);

INSERT INTO research_project
(project_no, project_name, project_type, leader_id, species_id, description,
 research_objective, research_content, started_at, ended_at, project_status, review_status,
 review_comment, reviewed_by, reviewed_at,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-RESEARCH-01', '石柱黄连全生命周期数字化研究', 'research', @researcher_id, @species_id,
 '围绕黄连采集、生长观测、图像识别、证据链和公开数字档案开展演示性研究。',
 '建立一条可追溯、可复核的黄连数字化研究演示链路。',
 '研究内容包括地图点位、三阶段生长数据、影像识别、Agent 补采和数字档案。',
 '2026-03-01 08:00:00', '2026-12-31 18:00:00', 'ongoing', 'approved',
 '课题方案与演示范围一致，同意开展。', @reviewer_id, '2026-03-01 09:00:00',
 1, 0, NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S05; demo_seed'),
('DEMO-HL-RESEARCH-02', '黄连适生环境与质量关联研究', 'research', @researcher_id, @species_id,
 '围绕不同海拔、湿度和光照条件下的黄连生长指标开展的已结题演示研究。',
 '归纳演示点位的环境指标与生长表现关联。',
 '对比七个黄连演示点的海拔、温湿度、土壤湿度和生长阶段记录。',
 '2026-01-10 08:00:00', '2026-06-30 18:00:00', 'completed', 'approved',
 '研究过程与评价记录完整，同意结题。', @reviewer_id, '2026-07-02 09:00:00',
 1, 0, NOW(), NOW(), @researcher_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO; completed research project; demo_seed')
ON DUPLICATE KEY UPDATE
 project_name=VALUES(project_name), project_type=VALUES(project_type), leader_id=VALUES(leader_id),
 species_id=VALUES(species_id), description=VALUES(description), research_objective=VALUES(research_objective),
 research_content=VALUES(research_content), started_at=VALUES(started_at), ended_at=VALUES(ended_at),
 project_status=VALUES(project_status), review_status='approved', review_comment=VALUES(review_comment),
 reviewed_by=@reviewer_id, reviewed_at=VALUES(reviewed_at), status=1, is_deleted=0,
 updated_at=NOW(), updated_by=@researcher_id, remark=VALUES(remark);

SET @project_id := (SELECT id FROM research_project WHERE project_no='DEMO-HL-RESEARCH-01' AND is_deleted=0 LIMIT 1);
SET @project_02_id := (SELECT id FROM research_project WHERE project_no='DEMO-HL-RESEARCH-02' AND is_deleted=0 LIMIT 1);

INSERT INTO rel_project_member
(project_id, user_id, member_role, member_status, invitation_status,
 joined_at, invited_by, invited_at, accepted_at, created_at, updated_at,
 created_by, updated_by, version, remark)
VALUES
(@project_id, @researcher_id, 'leader', 'active', 'accepted', '2026-03-01 08:00:00', @researcher_id, '2026-02-25 09:00:00', '2026-02-25 09:00:00', NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S06'),
(@project_id, @teacher_id, 'assistant', 'active', 'accepted', '2026-03-02 08:00:00', @researcher_id, '2026-02-26 09:00:00', '2026-02-26 10:00:00', NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S06'),
(@project_id, @student_id, 'student', 'active', 'accepted', '2026-03-03 08:00:00', @researcher_id, '2026-02-27 09:00:00', '2026-02-27 11:00:00', NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S06')
ON DUPLICATE KEY UPDATE
 member_role=VALUES(member_role), member_status='active', invitation_status='accepted',
 left_at=NULL, joined_at=VALUES(joined_at), updated_at=NOW(), updated_by=@researcher_id,
 remark=VALUES(remark);

INSERT INTO rel_project_course
(project_id, course_id, relation_type, is_primary, sort_order,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
SELECT @project_id, @course_id, 'foundation', 1, 1,
 1, 0, NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S05'
WHERE NOT EXISTS (
  SELECT 1 FROM rel_project_course WHERE project_id=@project_id AND course_id=@course_id AND is_deleted=0
);

INSERT INTO rel_project_species
(project_id, species_id, relation_type, is_primary, sort_order,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
SELECT @project_id, @species_id, 'research_object', 1, 1,
 1, 0, NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S05'
WHERE NOT EXISTS (
  SELECT 1 FROM rel_project_species WHERE project_id=@project_id AND species_id=@species_id AND is_deleted=0
);

-- 在正式素材就绪前，课题过程材料先复用现有黄连证据图；附件脚本会替换这两条临时绑定。
SET @fallback_file_01 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_WHOLE_01' LIMIT 1);
SET @fallback_file_02 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_LEAF_02' LIMIT 1);
SET @fallback_file_03 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_WHOLE_03' LIMIT 1);

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@fallback_file_01, 'research_project', @project_id, 'image', 0, 1, NOW(), @researcher_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S07'),
(@fallback_file_02, 'research_project', @project_id, 'image', 0, 2, NOW(), @researcher_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S07');

INSERT INTO edu_training_plan
(plan_no, plan_name, plan_type, owner_id, description, started_at, ended_at,
 publish_status, course_id, trainer_id, location, published_at, published_by, completion_criteria,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-TRAIN-01', '黄连规范化采集与影像记录培训', 'practice', @teacher_id,
 '培训内容包括黄连生长指标记录、整株/叶片/根茎拍摄规范、AI 识别和二维码档案核验。',
 '2026-06-20 08:00:00', '2026-06-21 18:00:00', 'published', @course_id, @admin_id,
 '石柱黄连科研 Agent 演示基地', '2026-06-15 09:00:00', @teacher_id,
 '完成必修资源并通过采集规范与影像证据检查。',
 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S08; demo_seed'),
('DEMO-HL-TRAIN-02', '黄连采集规范复训', 'practice', @teacher_id,
 '面向已完成采集任务的成员，复核点位记录、影像类型和批次审核规范。',
 '2026-05-08 08:00:00', '2026-05-09 18:00:00', 'published', @course_02_id, @admin_id,
 '石柱黄连科研 Agent 演示基地', '2026-04-30 09:00:00', @teacher_id,
 '完成采集规范复训并通过课后核验。',
 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; completed training plan; demo_seed')
ON DUPLICATE KEY UPDATE
 plan_name=VALUES(plan_name), plan_type=VALUES(plan_type), owner_id=VALUES(owner_id),
 description=VALUES(description), started_at=VALUES(started_at), ended_at=VALUES(ended_at),
 publish_status='published', course_id=VALUES(course_id), trainer_id=VALUES(trainer_id),
 location=VALUES(location), published_at=VALUES(published_at), published_by=VALUES(published_by),
 completion_criteria=VALUES(completion_criteria), status=1, is_deleted=0,
 updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

SET @training_plan_id := (SELECT id FROM edu_training_plan WHERE plan_no='DEMO-HL-TRAIN-01' AND is_deleted=0 LIMIT 1);
SET @training_plan_02_id := (SELECT id FROM edu_training_plan WHERE plan_no='DEMO-HL-TRAIN-02' AND is_deleted=0 LIMIT 1);

INSERT INTO edu_training_plan_item
(plan_id, item_type, item_title, description, course_id, project_id, base_id, species_id,
 is_required, completion_weight, sort_order, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @training_plan_id, 'course', '黄连生长观测与数字化鉴别实验', '培训基础课程', @course_id, NULL, NULL, NULL,
 1, 25.00, 1, 1, 0, NOW(), NOW(), @admin_id, @admin_id, 0, 'BDIS_DEFENSE_DEMO:S09'
WHERE NOT EXISTS (SELECT 1 FROM edu_training_plan_item WHERE plan_id=@training_plan_id AND item_type='course' AND course_id=@course_id AND is_deleted=0);
INSERT INTO edu_training_plan_item
(plan_id, item_type, item_title, description, course_id, project_id, base_id, species_id,
 is_required, completion_weight, sort_order, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @training_plan_id, 'project', '石柱黄连全生命周期数字化研究', '培训关联课题', NULL, @project_id, NULL, NULL,
 1, 25.00, 2, 1, 0, NOW(), NOW(), @admin_id, @admin_id, 0, 'BDIS_DEFENSE_DEMO:S09'
WHERE NOT EXISTS (SELECT 1 FROM edu_training_plan_item WHERE plan_id=@training_plan_id AND item_type='project' AND project_id=@project_id AND is_deleted=0);
INSERT INTO edu_training_plan_item
(plan_id, item_type, item_title, description, course_id, project_id, base_id, species_id,
 is_required, completion_weight, sort_order, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @training_plan_id, 'base', '石柱黄连科研 Agent 演示基地', '实践场地', NULL, NULL, @base_id, NULL,
 1, 25.00, 3, 1, 0, NOW(), NOW(), @admin_id, @admin_id, 0, 'BDIS_DEFENSE_DEMO:S09'
WHERE NOT EXISTS (SELECT 1 FROM edu_training_plan_item WHERE plan_id=@training_plan_id AND item_type='base' AND base_id=@base_id AND is_deleted=0);
INSERT INTO edu_training_plan_item
(plan_id, item_type, item_title, description, course_id, project_id, base_id, species_id,
 is_required, completion_weight, sort_order, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @training_plan_id, 'species', '黄连', '核心观测药材', NULL, NULL, NULL, @species_id,
 1, 25.00, 4, 1, 0, NOW(), NOW(), @admin_id, @admin_id, 0, 'BDIS_DEFENSE_DEMO:S09'
WHERE NOT EXISTS (SELECT 1 FROM edu_training_plan_item WHERE plan_id=@training_plan_id AND item_type='species' AND species_id=@species_id AND is_deleted=0);

INSERT INTO edu_training_record
(attendance_no, user_id, course_id, plan_id, progress, training_status, score,
 attendance_status, checked_in_at, started_at, completed_at, result_comment,
 created_at, updated_at, remark)
VALUES
('DEMO-HL-ATT-STUDENT', @student_id, @course_id, @training_plan_id, 100.00, 'completed', 92.00, 'present', '2026-06-20 08:05:00', '2026-06-20 08:05:00', '2026-06-21 16:30:00', '已完成采集指标、影像规范和档案核验。', NOW(), NOW(), 'BDIS_DEFENSE_DEMO:S10'),
('DEMO-HL-ATT-RESEARCHER', @researcher_id, @course_id, @training_plan_id, 100.00, 'completed', 88.00, 'present', '2026-06-20 08:02:00', '2026-06-20 08:02:00', '2026-06-21 16:20:00', '已完成科研证据采集与复核练习。', NOW(), NOW(), 'BDIS_DEFENSE_DEMO:S10'),
('DEMO-HL-ATT-TEACHER', @teacher_id, @course_id, @training_plan_id, 65.00, 'learning', NULL, 'present', '2026-06-20 08:00:00', '2026-06-20 08:00:00', NULL, '已完成课程与地图部分，继续学习数字档案环节。', NOW(), NOW(), 'BDIS_DEFENSE_DEMO:S10'),
('DEMO-HL-ATT-STUDENT-02', @student_id, @course_02_id, @training_plan_02_id, 100.00, 'completed', 94.00, 'present', '2026-05-08 08:05:00', '2026-05-08 08:05:00', '2026-05-09 16:40:00', '已完成采集规范复训和课后核验。', NOW(), NOW(), 'BDIS_DEFENSE_DEMO; completed training attendance; demo_seed')
ON DUPLICATE KEY UPDATE
 user_id=VALUES(user_id), course_id=VALUES(course_id), plan_id=VALUES(plan_id),
 progress=VALUES(progress), training_status=VALUES(training_status), score=VALUES(score),
 attendance_status=VALUES(attendance_status), checked_in_at=VALUES(checked_in_at),
 started_at=VALUES(started_at), completed_at=VALUES(completed_at),
 result_comment=VALUES(result_comment), updated_at=NOW(), remark=VALUES(remark);

SET @training_record_student := (SELECT id FROM edu_training_record WHERE attendance_no='DEMO-HL-ATT-STUDENT' LIMIT 1);
SET @training_record_researcher := (SELECT id FROM edu_training_record WHERE attendance_no='DEMO-HL-ATT-RESEARCHER' LIMIT 1);

INSERT INTO edu_training_feedback
(training_record_id, user_id, rating, feedback_content, submitted_at, created_at, updated_at, remark)
VALUES
(@training_record_student, @student_id, 5.00, '拍摄规范和指标检查清单很清晰，二维码档案便于回看三个阶段。', '2026-06-21 17:00:00', NOW(), NOW(), 'BDIS_DEFENSE_DEMO:S11'),
(@training_record_researcher, @researcher_id, 4.00, '建议继续强化低置信度图像的复拍与审核规范，整体培训实用。', '2026-06-21 17:10:00', NOW(), NOW(), 'BDIS_DEFENSE_DEMO:S11')
ON DUPLICATE KEY UPDATE
 rating=VALUES(rating), feedback_content=VALUES(feedback_content),
 submitted_at=VALUES(submitted_at), updated_at=NOW(), remark=VALUES(remark);

-- ---------------------------------------------------------------------------
-- E. 普通业务：评价指标、任务和结果
-- ---------------------------------------------------------------------------

INSERT INTO eval_indicator
(indicator_no, indicator_name, indicator_type, parent_id, weight, max_score,
 score_desc, sort_order, status, is_deleted, created_at, updated_at,
 created_by, updated_by, version, remark)
VALUES
('DEMO-HL-IND-01', '数据完整性', 'quality', 0, 30.00, 100.00, '评价生长指标、时间、坐标和审核信息的完整程度。', 1, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S12'),
('DEMO-HL-IND-02', '采集规范性', 'process', 0, 25.00, 100.00, '评价采集步骤、记录时序和审核留痕。', 2, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S12'),
('DEMO-HL-IND-03', '图像与识别证据质量', 'quality', 0, 25.00, 100.00, '评价整株、叶片和根茎图像以及识别置信度和复核状态。', 3, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S12'),
('DEMO-HL-IND-04', '档案可追溯性', 'result', 0, 20.00, 100.00, '评价任务级档案、审核链、证据链和公开访问设计。', 4, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S12')
ON DUPLICATE KEY UPDATE
 indicator_name=VALUES(indicator_name), indicator_type=VALUES(indicator_type), parent_id=0,
 weight=VALUES(weight), max_score=VALUES(max_score), score_desc=VALUES(score_desc),
 sort_order=VALUES(sort_order), status=1, is_deleted=0, updated_at=NOW(),
 updated_by=@teacher_id, remark=VALUES(remark);

SET @indicator_01 := (SELECT id FROM eval_indicator WHERE indicator_no='DEMO-HL-IND-01' AND is_deleted=0 LIMIT 1);
SET @indicator_02 := (SELECT id FROM eval_indicator WHERE indicator_no='DEMO-HL-IND-02' AND is_deleted=0 LIMIT 1);
SET @indicator_03 := (SELECT id FROM eval_indicator WHERE indicator_no='DEMO-HL-IND-03' AND is_deleted=0 LIMIT 1);
SET @indicator_04 := (SELECT id FROM eval_indicator WHERE indicator_no='DEMO-HL-IND-04' AND is_deleted=0 LIMIT 1);

INSERT INTO eval_task
(task_no, task_name, task_type, target_type, target_id, owner_id,
 started_at, ended_at, task_status, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-EVAL-01', '石柱黄连数字化研究质量评价', 'comprehensive',
 'herb_species', @species_id, @teacher_id, '2026-07-10 08:00:00', '2026-07-12 18:00:00',
 'confirmed', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S13; demo_seed'),
('DEMO-HL-EVAL-02', '黄连规范化采集质量评价', 'process',
 'herb_distribution', @point_01, @teacher_id, '2026-06-24 08:00:00', '2026-06-25 18:00:00',
 'confirmed', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation; demo_seed'),
('DEMO-HL-EVAL-03', '黄连数字档案完整性评价', 'quality',
 'research_project', @project_id, @teacher_id, '2026-06-28 08:00:00', '2026-06-29 18:00:00',
 'confirmed', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation; demo_seed')
ON DUPLICATE KEY UPDATE
 task_name=VALUES(task_name), task_type=VALUES(task_type), target_type=VALUES(target_type),
 target_id=VALUES(target_id), owner_id=VALUES(owner_id), started_at=VALUES(started_at),
 ended_at=VALUES(ended_at), task_status='confirmed', status=1, is_deleted=0,
 updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

SET @eval_task_id := (SELECT id FROM eval_task WHERE task_no='DEMO-HL-EVAL-01' AND is_deleted=0 LIMIT 1);
SET @eval_task_collection_id := (SELECT id FROM eval_task WHERE task_no='DEMO-HL-EVAL-02' AND is_deleted=0 LIMIT 1);
SET @eval_task_archive_id := (SELECT id FROM eval_task WHERE task_no='DEMO-HL-EVAL-03' AND is_deleted=0 LIMIT 1);

INSERT INTO eval_score_record
(task_id, indicator_id, evaluator_id, score, score_comment, scored_at,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
(@eval_task_id, @indicator_01, @teacher_id, 94.00, '三阶段指标与时序基本完整，第三阶段缺口已被明确标记。', '2026-07-12 09:00:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S14'),
(@eval_task_id, @indicator_02, @teacher_id, 90.00, '采集、提交与审核时序清晰，演示标识完整。', '2026-07-12 09:05:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S14'),
(@eval_task_id, @indicator_03, @teacher_id, 88.00, '影像类型可追溯，第三阶段低置信度与缺图风险已显式保留。', '2026-07-12 09:10:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S14'),
(@eval_task_id, @indicator_04, @teacher_id, 92.00, '审核留痕、任务级档案和公开页设计完整。', '2026-07-12 09:15:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S14'),
(@eval_task_collection_id, @indicator_01, @teacher_id, 93.00, '采集点位、时间和环境指标完整。', '2026-06-25 09:00:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_collection_id, @indicator_02, @teacher_id, 95.00, '采集、提交和审核流程符合规范。', '2026-06-25 09:05:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_collection_id, @indicator_03, @teacher_id, 91.00, '现场影像类型和采集位置可追溯。', '2026-06-25 09:10:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_collection_id, @indicator_04, @teacher_id, 90.00, '采集链路与审核记录完整。', '2026-06-25 09:15:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_archive_id, @indicator_01, @teacher_id, 96.00, '任务级档案要素齐全。', '2026-06-29 09:00:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_archive_id, @indicator_02, @teacher_id, 92.00, '过程留痕和审核节点清晰。', '2026-06-29 09:05:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_archive_id, @indicator_03, @teacher_id, 94.00, '影像、识别与证据链关联完整。', '2026-06-29 09:10:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_archive_id, @indicator_04, @teacher_id, 97.00, '二维码公开页和档案核验结果可复查。', '2026-06-29 09:15:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation')
ON DUPLICATE KEY UPDATE
 score=VALUES(score), score_comment=VALUES(score_comment), scored_at=VALUES(scored_at),
 status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

INSERT INTO eval_result
(task_id, total_score, result_level, result_desc, confirmed_by, confirmed_at,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
(@eval_task_id, 91.10, 'excellent', '数据完整性 94、采集规范性 90、图像与识别证据质量 88、档案可追溯性 92；加权总分 91.10，等级优秀。',
 @teacher_id, '2026-07-12 10:00:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S14; demo_seed'),
(@eval_task_collection_id, 92.40, 'excellent', '黄连采集点位、环境指标、影像证据和审核链完整，评价等级优秀。',
 @teacher_id, '2026-06-25 10:00:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation'),
(@eval_task_archive_id, 94.70, 'excellent', '黄连数字档案、二维码公开页、证据链和审核记录完整，评价等级优秀。',
 @teacher_id, '2026-06-29 10:00:00', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO; confirmed evaluation')
ON DUPLICATE KEY UPDATE
 total_score=VALUES(total_score), result_level='excellent', result_desc=VALUES(result_desc),
 confirmed_by=@teacher_id, confirmed_at=VALUES(confirmed_at), status=1, is_deleted=0,
 updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

-- ---------------------------------------------------------------------------
-- F. 普通业务：申报档案和业绩认定
-- ---------------------------------------------------------------------------

INSERT INTO eval_application
(application_no, application_title, application_type, applicant_id, review_status,
 submitted_at, reviewer_id, reviewed_at, review_comment,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-DECL-PENDING', '石柱黄连数字化采集实践成果申报', 'research_project', @student_id, 'submitted',
 '2026-07-13 09:00:00', NULL, NULL, NULL,
 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S15; demo_seed'),
('DEMO-HL-DECL-APPROVED', '石柱黄连全生命周期数字化研究成果申报', 'research_project', @student_id, 'approved',
 '2026-07-10 09:00:00', @reviewer_id, '2026-07-11 10:00:00', '申报材料完整，能够对应课程、课题、培训和评价结果，同意通过并生成档案袋。',
 1, 0, NOW(), NOW(), @student_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:S16; demo_seed'),
('DEMO-HL-DECL-APPROVED-02', '黄连规范化采集实践成果申报', 'teaching', @student_id, 'approved',
 '2026-06-26 09:00:00', @reviewer_id, '2026-06-27 10:00:00', '课程实践、采集记录和评价结果对应清晰，同意通过。',
 1, 0, NOW(), NOW(), @student_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO; approved declaration; demo_seed'),
('DEMO-HL-DECL-APPROVED-03', '黄连数字档案应用成果申报', 'research_project', @researcher_id, 'approved',
 '2026-06-30 09:00:00', @reviewer_id, '2026-07-01 10:00:00', '数字档案、公开溯源和证据链材料完备，同意通过。',
 1, 0, NOW(), NOW(), @researcher_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO; approved declaration; demo_seed')
ON DUPLICATE KEY UPDATE
 application_title=VALUES(application_title), application_type=VALUES(application_type),
 applicant_id=VALUES(applicant_id), review_status=VALUES(review_status),
 submitted_at=VALUES(submitted_at), reviewer_id=VALUES(reviewer_id), reviewed_at=VALUES(reviewed_at),
 review_comment=VALUES(review_comment), status=1, is_deleted=0, updated_at=NOW(),
 updated_by=VALUES(updated_by), remark=VALUES(remark);

SET @declaration_pending_id := (SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-PENDING' AND is_deleted=0 LIMIT 1);
SET @declaration_approved_id := (SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-APPROVED' AND is_deleted=0 LIMIT 1);
SET @declaration_approved_02_id := (SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-APPROVED-02' AND is_deleted=0 LIMIT 1);
SET @declaration_approved_03_id := (SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-APPROVED-03' AND is_deleted=0 LIMIT 1);

INSERT INTO eval_review_record
(application_id, reviewer_id, review_action, before_status, review_status,
 review_comment, reviewed_at, created_at, created_by, remark)
SELECT @declaration_pending_id, @student_id, 'submit', 'draft', 'submitted',
 '申报人已提交三份成果材料。', '2026-07-13 09:00:00', '2026-07-13 09:00:00', @student_id, 'BDIS_DEFENSE_DEMO:S15:submit'
WHERE NOT EXISTS (
 SELECT 1 FROM eval_review_record WHERE application_id=@declaration_pending_id AND review_action='submit' AND remark='BDIS_DEFENSE_DEMO:S15:submit'
);
INSERT INTO eval_review_record
(application_id, reviewer_id, review_action, before_status, review_status,
 review_comment, reviewed_at, created_at, created_by, remark)
SELECT @declaration_approved_id, @student_id, 'submit', 'draft', 'submitted',
 '申报人已提交黄连全生命周期数字化研究成果。', '2026-07-10 09:00:00', '2026-07-10 09:00:00', @student_id, 'BDIS_DEFENSE_DEMO:S16:submit'
WHERE NOT EXISTS (
 SELECT 1 FROM eval_review_record WHERE application_id=@declaration_approved_id AND review_action='submit' AND remark='BDIS_DEFENSE_DEMO:S16:submit'
);
INSERT INTO eval_review_record
(application_id, reviewer_id, review_action, before_status, review_status,
 review_comment, reviewed_at, created_at, created_by, remark)
SELECT @declaration_approved_id, @reviewer_id, 'approve', 'submitted', 'approved',
 '申报材料完整，能够对应课程、课题、培训和评价结果，同意通过并生成档案袋。',
 '2026-07-11 10:00:00', '2026-07-11 10:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:S16:approve'
WHERE NOT EXISTS (
 SELECT 1 FROM eval_review_record WHERE application_id=@declaration_approved_id AND review_action='approve' AND remark='BDIS_DEFENSE_DEMO:S16:approve'
);
INSERT INTO eval_review_record
(application_id, reviewer_id, review_action, before_status, review_status,
 review_comment, reviewed_at, created_at, created_by, remark)
SELECT @declaration_approved_02_id, @reviewer_id, 'approve', 'submitted', 'approved',
 '课程实践、采集记录和评价结果对应清晰，同意通过。',
 '2026-06-27 10:00:00', '2026-06-27 10:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:DECL02:approve'
WHERE NOT EXISTS (
 SELECT 1 FROM eval_review_record WHERE application_id=@declaration_approved_02_id AND review_action='approve' AND remark='BDIS_DEFENSE_DEMO:DECL02:approve'
);
INSERT INTO eval_review_record
(application_id, reviewer_id, review_action, before_status, review_status,
 review_comment, reviewed_at, created_at, created_by, remark)
SELECT @declaration_approved_03_id, @reviewer_id, 'approve', 'submitted', 'approved',
 '数字档案、公开溯源和证据链材料完备，同意通过。',
 '2026-07-01 10:00:00', '2026-07-01 10:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:DECL03:approve'
WHERE NOT EXISTS (
 SELECT 1 FROM eval_review_record WHERE application_id=@declaration_approved_03_id AND review_action='approve' AND remark='BDIS_DEFENSE_DEMO:DECL03:approve'
);

-- 素材未就绪前使用现有黄连影像保证申报材料数量和文件访问均有效；附件脚本会替换为最终文件。
INSERT INTO eval_attachment
(application_id, file_id, file_name, file_type, file_url, file_size,
 uploader_id, uploaded_at, status, is_deleted, created_at, updated_at,
 created_by, updated_by, version, remark)
SELECT @declaration_pending_id, f.id, f.original_filename, f.file_type, f.file_url, f.file_size,
 @student_id, '2026-07-13 08:30:00', 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:FALLBACK:S15'
FROM sys_file_resource f
WHERE f.id IN (@fallback_file_01,@fallback_file_02,@fallback_file_03)
  AND NOT EXISTS (
    SELECT 1 FROM eval_attachment a
    WHERE a.application_id=@declaration_pending_id AND a.file_id=f.id AND a.is_deleted=0
  );
INSERT INTO eval_attachment
(application_id, file_id, file_name, file_type, file_url, file_size,
 uploader_id, uploaded_at, status, is_deleted, created_at, updated_at,
 created_by, updated_by, version, remark)
SELECT @declaration_approved_id, f.id, f.original_filename, f.file_type, f.file_url, f.file_size,
 @student_id, '2026-07-10 08:30:00', 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:FALLBACK:S16'
FROM sys_file_resource f
WHERE f.id IN (@fallback_file_01,@fallback_file_02,@fallback_file_03)
  AND NOT EXISTS (
    SELECT 1 FROM eval_attachment a
    WHERE a.application_id=@declaration_approved_id AND a.file_id=f.id AND a.is_deleted=0
  );

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
SELECT f.id, 'eval_application', @declaration_pending_id, 'application_material', 0,
 CASE f.id WHEN @fallback_file_01 THEN 1 WHEN @fallback_file_02 THEN 2 ELSE 3 END,
 NOW(), @student_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S15'
FROM sys_file_resource f WHERE f.id IN (@fallback_file_01,@fallback_file_02,@fallback_file_03);
INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
SELECT f.id, 'eval_application', @declaration_approved_id, 'application_material', 0,
 CASE f.id WHEN @fallback_file_01 THEN 1 WHEN @fallback_file_02 THEN 2 ELSE 3 END,
 NOW(), @student_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S16'
FROM sys_file_resource f WHERE f.id IN (@fallback_file_01,@fallback_file_02,@fallback_file_03);

INSERT INTO eval_archive
(archive_no, application_id, archive_title, owner_id, archive_status, generated_at,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-ARCHIVE-APPROVED', @declaration_approved_id, '石柱黄连全生命周期数字化研究成果申报档案袋',
 @student_id, 'generated', '2026-07-11 10:01:00', 1, 0, NOW(), NOW(), @reviewer_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:S16; generated from approved application')
ON DUPLICATE KEY UPDATE
 archive_title=VALUES(archive_title), owner_id=VALUES(owner_id), archive_status='generated',
 generated_at=VALUES(generated_at), status=1, is_deleted=0, updated_at=NOW(),
 updated_by=@reviewer_id, remark=VALUES(remark);

SET @declaration_archive_id := (SELECT id FROM eval_archive WHERE application_id=@declaration_approved_id LIMIT 1);

INSERT INTO eval_archive_item
(archive_id, source_type, source_id, item_name, item_desc, sort_order, created_at, created_by, remark)
SELECT @declaration_archive_id, 'attachment', a.id, a.file_name, '申报附件', 10+a.id, NOW(), @reviewer_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S16'
FROM eval_attachment a
WHERE a.application_id=@declaration_approved_id AND a.is_deleted=0
  AND NOT EXISTS (
    SELECT 1 FROM eval_archive_item i
    WHERE i.archive_id=@declaration_archive_id AND i.source_type='attachment' AND i.source_id=a.id
  );
INSERT INTO eval_archive_item
(archive_id, source_type, source_id, item_name, item_desc, sort_order, created_at, created_by, remark)
SELECT @declaration_archive_id, 'eval_task', @eval_task_id, '石柱黄连数字化研究质量评价', '评价任务与优秀结果', 21, NOW(), @reviewer_id, 'BDIS_DEFENSE_DEMO:S16'
WHERE NOT EXISTS (SELECT 1 FROM eval_archive_item WHERE archive_id=@declaration_archive_id AND source_type='eval_task' AND source_id=@eval_task_id);
INSERT INTO eval_archive_item
(archive_id, source_type, source_id, item_name, item_desc, sort_order, created_at, created_by, remark)
SELECT @declaration_archive_id, 'research_project', @project_id, '石柱黄连全生命周期数字化研究', '关联科研课题', 22, NOW(), @reviewer_id, 'BDIS_DEFENSE_DEMO:S16'
WHERE NOT EXISTS (SELECT 1 FROM eval_archive_item WHERE archive_id=@declaration_archive_id AND source_type='research_project' AND source_id=@project_id);
INSERT INTO eval_archive_item
(archive_id, source_type, source_id, item_name, item_desc, sort_order, created_at, created_by, remark)
SELECT @declaration_archive_id, 'edu_course', @course_id, '黄连生长观测与数字化鉴别实验', '关联实验课程', 23, NOW(), @reviewer_id, 'BDIS_DEFENSE_DEMO:S16'
WHERE NOT EXISTS (SELECT 1 FROM eval_archive_item WHERE archive_id=@declaration_archive_id AND source_type='edu_course' AND source_id=@course_id);
INSERT INTO eval_archive_item
(archive_id, source_type, source_id, item_name, item_desc, sort_order, created_at, created_by, remark)
SELECT @declaration_archive_id, 'edu_training_plan', @training_plan_id, '黄连规范化采集与影像记录培训', '关联培训计划', 24, NOW(), @reviewer_id, 'BDIS_DEFENSE_DEMO:S16'
WHERE NOT EXISTS (SELECT 1 FROM eval_archive_item WHERE archive_id=@declaration_archive_id AND source_type='edu_training_plan' AND source_id=@training_plan_id);

INSERT INTO perf_standard
(standard_no, standard_version, standard_name, performance_type, standard_desc,
 score_rule, level_rule, effective_from, effective_to, material_required,
 min_material_count, published_at, published_by, sort_order, status, lifecycle_status,
 is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO-HL-PERF-STD', 1, '科研数字化成果认定标准', 'RESEARCH',
 '用于认定具有可追溯数据、影像证据、评价结果和数字档案的科研数字化成果。',
 '审核数据完整性、证据可追溯性、研究过程和成果应用价值。',
 '材料完整且审核通过认定为校级科研成果。',
 '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1, 2,
 '2026-01-05 09:00:00', @teacher_id, 1, 1, 'published',
 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S17; demo_seed')
ON DUPLICATE KEY UPDATE
 standard_name=VALUES(standard_name), performance_type=VALUES(performance_type),
 standard_desc=VALUES(standard_desc), score_rule=VALUES(score_rule), level_rule=VALUES(level_rule),
 effective_from=VALUES(effective_from), effective_to=VALUES(effective_to),
 material_required=1, min_material_count=2, published_at=VALUES(published_at),
 published_by=@teacher_id, sort_order=1, status=1, lifecycle_status='published',
 is_deleted=0, updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

SET @performance_standard_id := (
 SELECT id FROM perf_standard WHERE standard_no='DEMO-HL-PERF-STD' AND standard_version=1 AND is_deleted=0 LIMIT 1
);
SET @standard_snapshot := CONCAT(
 '认定规则：审核数据完整性、证据可追溯性、研究过程和成果应用价值。', CHAR(10),
 '等级规则：材料完整且审核通过认定为校级科研成果。', CHAR(10),
 '佐证材料：至少需要 2 份材料'
);

INSERT INTO perf_record
(performance_no, user_id, performance_title, performance_type, performance_level, occurred_at,
 standard_id, standard_no_snapshot, standard_version_snapshot, standard_name_snapshot,
 standard_rule_snapshot, source_type, source_id, source_name_snapshot,
 identify_status, submitted_at, status, is_deleted, created_at, updated_at,
 created_by, updated_by, version, remark)
VALUES
('DEMO-HL-PERF-PENDING', @student_id, '石柱黄连采集与数字档案建设实践', 'RESEARCH', '校级', '2026-07-11 10:00:00',
 @performance_standard_id, 'DEMO-HL-PERF-STD', 1, '科研数字化成果认定标准', @standard_snapshot,
 'eval_application', @declaration_approved_id, '石柱黄连全生命周期数字化研究成果申报',
 'submitted', '2026-07-14 09:00:00', 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S18; demo_seed'),
('DEMO-HL-PERF-APPROVED', @student_id, '石柱黄连全生命周期数字化研究成果', 'RESEARCH', '校级', '2026-07-11 10:00:00',
 @performance_standard_id, 'DEMO-HL-PERF-STD', 1, '科研数字化成果认定标准', @standard_snapshot,
 'eval_application', @declaration_approved_id, '石柱黄连全生命周期数字化研究成果申报',
 'approved', '2026-07-12 09:00:00', 1, 0, NOW(), NOW(), @student_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO:S19; demo_seed'),
('DEMO-HL-PERF-APPROVED-02', @student_id, '黄连规范化采集教学实践成果', 'TEACHING', '院级', '2026-06-27 10:00:00',
 @performance_standard_id, 'DEMO-HL-PERF-STD', 1, '科研数字化成果认定标准', @standard_snapshot,
 'eval_application', @declaration_approved_02_id, '黄连规范化采集实践成果申报',
 'approved', '2026-06-28 09:00:00', 1, 0, NOW(), NOW(), @student_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO; approved performance; demo_seed'),
('DEMO-HL-PERF-APPROVED-03', @researcher_id, '黄连数字档案与溯源应用成果', 'RESEARCH', '校级', '2026-07-01 10:00:00',
 @performance_standard_id, 'DEMO-HL-PERF-STD', 1, '科研数字化成果认定标准', @standard_snapshot,
 'eval_application', @declaration_approved_03_id, '黄连数字档案应用成果申报',
 'approved', '2026-07-02 09:00:00', 1, 0, NOW(), NOW(), @researcher_id, @reviewer_id, 0, 'BDIS_DEFENSE_DEMO; approved performance; demo_seed')
ON DUPLICATE KEY UPDATE
 user_id=VALUES(user_id), performance_title=VALUES(performance_title),
 performance_type=VALUES(performance_type), performance_level=VALUES(performance_level),
 occurred_at=VALUES(occurred_at), standard_id=VALUES(standard_id),
 standard_no_snapshot=VALUES(standard_no_snapshot), standard_version_snapshot=1,
 standard_name_snapshot=VALUES(standard_name_snapshot), standard_rule_snapshot=VALUES(standard_rule_snapshot),
 source_type=VALUES(source_type), source_id=VALUES(source_id),
 source_name_snapshot=VALUES(source_name_snapshot), identify_status=VALUES(identify_status),
 submitted_at=VALUES(submitted_at), status=1, is_deleted=0, updated_at=NOW(),
 updated_by=VALUES(updated_by), remark=VALUES(remark);

SET @performance_pending_id := (SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-PENDING' AND is_deleted=0 LIMIT 1);
SET @performance_approved_id := (SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-APPROVED' AND is_deleted=0 LIMIT 1);
SET @performance_approved_02_id := (SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-APPROVED-02' AND is_deleted=0 LIMIT 1);
SET @performance_approved_03_id := (SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-APPROVED-03' AND is_deleted=0 LIMIT 1);

INSERT IGNORE INTO perf_participant
(performance_id, user_id, participant_role, sort_order, is_primary,
 status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
(@performance_pending_id, @student_id, 'owner', 0, 1, 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S18'),
(@performance_pending_id, @researcher_id, 'participant', 1, 0, 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S18'),
(@performance_pending_id, @teacher_id, 'participant', 2, 0, 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S18'),
(@performance_approved_id, @student_id, 'owner', 0, 1, 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S19'),
(@performance_approved_id, @researcher_id, 'participant', 1, 0, 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S19'),
(@performance_approved_id, @teacher_id, 'participant', 2, 0, 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S19');

INSERT INTO perf_identification
(performance_id, identifier_id, identify_action, identify_result, identify_comment,
 identified_at, created_at, created_by, remark)
SELECT @performance_pending_id, @student_id, 'submit', 'submitted', '业绩材料和认定标准快照已冻结。',
 '2026-07-14 09:00:00', '2026-07-14 09:00:00', @student_id, 'BDIS_DEFENSE_DEMO:S18:submit'
WHERE NOT EXISTS (SELECT 1 FROM perf_identification WHERE performance_id=@performance_pending_id AND identify_action='submit' AND remark='BDIS_DEFENSE_DEMO:S18:submit');
INSERT INTO perf_identification
(performance_id, identifier_id, identify_action, identify_result, identify_comment,
 identified_at, created_at, created_by, remark)
SELECT @performance_approved_id, @student_id, 'submit', 'submitted', '业绩材料和认定标准快照已冻结。',
 '2026-07-12 09:00:00', '2026-07-12 09:00:00', @student_id, 'BDIS_DEFENSE_DEMO:S19:submit'
WHERE NOT EXISTS (SELECT 1 FROM perf_identification WHERE performance_id=@performance_approved_id AND identify_action='submit' AND remark='BDIS_DEFENSE_DEMO:S19:submit');
INSERT INTO perf_identification
(performance_id, identifier_id, identify_action, identify_result, identify_comment,
 identified_at, created_at, created_by, remark)
SELECT @performance_approved_id, @reviewer_id, 'approve', 'approved', '来源申报、参与人、佐证材料与标准快照完整，同意认定。',
 '2026-07-13 10:00:00', '2026-07-13 10:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:S19:approve'
WHERE NOT EXISTS (SELECT 1 FROM perf_identification WHERE performance_id=@performance_approved_id AND identify_action='approve' AND remark='BDIS_DEFENSE_DEMO:S19:approve');
INSERT INTO perf_identification
(performance_id, identifier_id, identify_action, identify_result, identify_comment,
 identified_at, created_at, created_by, remark)
SELECT @performance_approved_02_id, @reviewer_id, 'approve', 'approved', '课程实践来源与材料快照完整，同意认定。',
 '2026-06-28 10:00:00', '2026-06-28 10:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:PERF02:approve'
WHERE NOT EXISTS (SELECT 1 FROM perf_identification WHERE performance_id=@performance_approved_02_id AND identify_action='approve' AND remark='BDIS_DEFENSE_DEMO:PERF02:approve');
INSERT INTO perf_identification
(performance_id, identifier_id, identify_action, identify_result, identify_comment,
 identified_at, created_at, created_by, remark)
SELECT @performance_approved_03_id, @reviewer_id, 'approve', 'approved', '数字档案应用成果来源完整，同意认定。',
 '2026-07-02 10:00:00', '2026-07-02 10:00:00', @reviewer_id, 'BDIS_DEFENSE_DEMO:PERF03:approve'
WHERE NOT EXISTS (SELECT 1 FROM perf_identification WHERE performance_id=@performance_approved_03_id AND identify_action='approve' AND remark='BDIS_DEFENSE_DEMO:PERF03:approve');

-- 临时使用两张黄连证据图满足标准的“至少 2 份材料”业务不变式；素材脚本会替换为成果说明和档案摘要。
INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@fallback_file_01, 'perf_record', @performance_pending_id, 'material', 0, 1, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S18'),
(@fallback_file_02, 'perf_record', @performance_pending_id, 'material', 0, 2, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S18'),
(@fallback_file_01, 'perf_record', @performance_approved_id, 'material', 0, 1, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S19'),
(@fallback_file_02, 'perf_record', @performance_approved_id, 'material', 0, 2, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:FALLBACK:S19');

COMMIT;

SELECT
 @teacher_id AS teacher_id,
 @collector_id AS collector_id,
 @reviewer_id AS reviewer_id,
 @species_id AS herb_species_id,
 @task_id AS collection_task_id,
 @course_id AS course_id,
 @project_id AS research_project_id,
 @training_plan_id AS training_plan_id,
 @eval_task_id AS evaluation_task_id,
 @declaration_pending_id AS pending_declaration_id,
 @declaration_approved_id AS approved_declaration_id,
 @performance_pending_id AS pending_performance_id,
 @performance_approved_id AS approved_performance_id;
