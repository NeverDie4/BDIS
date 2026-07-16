-- 本草数字孪生科研 Agent 基础演示数据（开发环境专用）。
-- 所有记录均以 demo_seed / 科研演示样本明确标记，不代表现场实测或科研结论。
-- 登录账号：agent_teacher_demo / password；collector_agent_demo / password。

INSERT INTO sys_user
(user_no, username, password_hash, real_name, user_type, status, is_deleted, created_at, updated_at, version, remark)
VALUES
('DEMO_AGENT_TEACHER', 'agent_teacher_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '科研 Agent 演示教师', 'teacher', 1, 0, NOW(), NOW(), 0, 'research agent demo seed'),
('DEMO_AGENT_COLLECTOR', 'collector_agent_demo', '$2a$10$6bvbc4YcNNJ8G04ht4nF8.gyQlisoJ55a/yX/LTwdigeEApavMgpK', '科研 Agent 演示采集员', 'collector', 1, 0, NOW(), NOW(), 0, 'research agent demo seed')
ON DUPLICATE KEY UPDATE real_name=VALUES(real_name), status=1, is_deleted=0, updated_at=NOW(), remark=VALUES(remark);

INSERT IGNORE INTO rel_user_role (user_id, role_id, created_at, remark)
SELECT u.id, r.id, NOW(), 'research agent demo seed'
FROM sys_user u JOIN auth_role r ON r.role_code = 'TEACHER'
WHERE u.username = 'agent_teacher_demo';
INSERT IGNORE INTO rel_user_role (user_id, role_id, created_at, remark)
SELECT u.id, r.id, NOW(), 'research agent demo seed'
FROM sys_user u JOIN auth_role r ON r.role_code = 'COLLECTOR'
WHERE u.username = 'collector_agent_demo';

SET @teacher_id := (SELECT id FROM sys_user WHERE username='agent_teacher_demo' AND is_deleted=0 LIMIT 1);
SET @collector_id := (SELECT id FROM sys_user WHERE username='collector_agent_demo' AND is_deleted=0 LIMIT 1);

INSERT INTO herb_species
(herb_no, herb_name, medicinal_part, origin_area, description, status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES ('DEMO_AGENT_HUANGLIAN', '黄连', '根茎', '重庆石柱', '科研 Agent 连续观测演示药材；非正式科研样本。', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'research agent demo seed')
ON DUPLICATE KEY UPDATE herb_name=VALUES(herb_name), description=VALUES(description), status=1, is_deleted=0, updated_at=NOW(), remark=VALUES(remark);

INSERT INTO herb_base
(base_no, base_name, base_type, address, contact_name, description, status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES ('DEMO_AGENT_BASE', '石柱黄连科研 Agent 演示基地', 'planting', '重庆市石柱县（演示地址）', '科研 Agent 演示采集员', '仅用于科研 Agent 工作流演示。', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'research agent demo seed')
ON DUPLICATE KEY UPDATE base_name=VALUES(base_name), description=VALUES(description), status=1, is_deleted=0, updated_at=NOW(), remark=VALUES(remark);

SET @species_id := (SELECT id FROM herb_species WHERE herb_no='DEMO_AGENT_HUANGLIAN' AND is_deleted=0 LIMIT 1);
SET @base_id := (SELECT id FROM herb_base WHERE base_no='DEMO_AGENT_BASE' AND is_deleted=0 LIMIT 1);

INSERT INTO herb_collection_task
(task_code, task_name, species_id, species_name, base_id, base_name, collect_place, planned_start_time, planned_end_time, collector_id, collector_name, task_status, description, status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES ('DEMO_AGENT_HL_CONTINUOUS', '黄连三阶段连续观测科研演示任务', @species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', '科研 Agent 演示样方（非现场实测坐标）', '2026-06-01 08:00:00', '2026-07-10 18:00:00', @collector_id, '科研 Agent 演示采集员', 'completed', '三阶段演示数据：第三阶段缺根茎图片且识别置信度偏低，用于触发证据缺口和复测方案。', 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'research agent demo seed; coordinates are DEMO, not field-measured')
ON DUPLICATE KEY UPDATE task_name=VALUES(task_name), species_id=VALUES(species_id), base_id=VALUES(base_id), collector_id=VALUES(collector_id), collector_name=VALUES(collector_name), task_status='completed', description=VALUES(description), status=1, is_deleted=0, updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);
SET @task_id := (SELECT id FROM herb_collection_task WHERE task_code='DEMO_AGENT_HL_CONTINUOUS' AND is_deleted=0 LIMIT 1);

INSERT INTO herb_batch
(batch_code, batch_name, task_id, species_id, species_name, base_id, base_name, origin_place, collect_start_time, collect_end_time, production_date, batch_status, image_count, identified_count, reviewed_count, need_review_count, evaluation_summary, status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO_AGENT_HL_STAGE_01', '黄连返青阶段观测', @task_id, @species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', '科研 Agent 演示样方', '2026-06-01 09:00:00', '2026-06-01 10:00:00', '2026-06-01', 'completed', 1, 1, 1, 0, '阶段一指标完整，包含整株演示图片。', 1, 0, NOW(), NOW(), @collector_id, @collector_id, 0, 'demo_seed; non-field-measured'),
('DEMO_AGENT_HL_STAGE_02', '黄连展叶阶段观测', @task_id, @species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', '科研 Agent 演示样方', '2026-06-18 09:00:00', '2026-06-18 10:00:00', '2026-06-18', 'completed', 1, 1, 1, 0, '阶段二土壤湿度下降，包含叶片演示图片。', 1, 0, NOW(), NOW(), @collector_id, @collector_id, 0, 'demo_seed; non-field-measured'),
('DEMO_AGENT_HL_STAGE_03', '黄连旺盛生长期观测', @task_id, @species_id, '黄连', @base_id, '石柱黄连科研 Agent 演示基地', '科研 Agent 演示样方', '2026-07-05 09:00:00', '2026-07-05 10:00:00', '2026-07-05', 'completed', 1, 1, 0, 1, '阶段三株高增量下降，缺根茎图片且识别置信度偏低。', 1, 0, NOW(), NOW(), @collector_id, @collector_id, 0, 'demo_seed; non-field-measured')
ON DUPLICATE KEY UPDATE task_id=VALUES(task_id), batch_name=VALUES(batch_name), batch_status=VALUES(batch_status), evaluation_summary=VALUES(evaluation_summary), status=1, is_deleted=0, updated_at=NOW(), remark=VALUES(remark);

SET @batch1 := (SELECT id FROM herb_batch WHERE batch_code='DEMO_AGENT_HL_STAGE_01' LIMIT 1);
SET @batch2 := (SELECT id FROM herb_batch WHERE batch_code='DEMO_AGENT_HL_STAGE_02' LIMIT 1);
SET @batch3 := (SELECT id FROM herb_batch WHERE batch_code='DEMO_AGENT_HL_STAGE_03' LIMIT 1);

INSERT INTO herb_growth_record
(batch_id, task_id, species_id, species_name, base_id, base_name, collector_id, collector_name_snapshot, longitude, latitude, growth_stage, plant_height, stem_diameter, soil_ph, temperature, humidity, soil_moisture, light, leaf_color, flowering_status, growth_evaluation, device_type, data_source, review_status, submitted_at, reviewed_at, collected_at, status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
VALUES
(@batch1,@task_id,@species_id,'黄连',@base_id,'石柱黄连科研 Agent 演示基地',@collector_id,'科研 Agent 演示采集员',108.2450000,30.1840000,'返青期',8.40,2.10,6.20,18.50,72.00,46.00,12500,'嫩绿','未开花','演示阶段一记录。','app','demo_seed','approved','2026-06-01 10:10:00','2026-06-02 09:00:00','2026-06-01 09:20:00',1,0,NOW(),NOW(),@collector_id,@teacher_id,0,'DEMO_COORDINATE; not field-measured'),
(@batch2,@task_id,@species_id,'黄连',@base_id,'石柱黄连科研 Agent 演示基地',@collector_id,'科研 Agent 演示采集员',108.2450000,30.1840000,'展叶期',13.20,2.80,6.10,22.30,66.00,32.00,14200,'绿色','未开花','土壤湿度较阶段一下降，仅描述演示数据事实。','app','demo_seed','approved','2026-06-18 10:10:00','2026-06-19 09:00:00','2026-06-18 09:20:00',1,0,NOW(),NOW(),@collector_id,@teacher_id,0,'DEMO_COORDINATE; not field-measured'),
(@batch3,@task_id,@species_id,'黄连',@base_id,'石柱黄连科研 Agent 演示基地',@collector_id,'科研 Agent 演示采集员',108.2450000,30.1840000,'旺盛生长期',15.00,3.10,NULL,24.60,63.00,25.00,15000,'深绿','未开花','株高增量下降；土壤 pH 未记录，等待科研 Agent 设计复测。','app','demo_seed','approved','2026-07-05 10:10:00','2026-07-06 09:00:00','2026-07-05 09:20:00',1,0,NOW(),NOW(),@collector_id,@teacher_id,0,'DEMO_COORDINATE; not field-measured')
ON DUPLICATE KEY UPDATE plant_height=VALUES(plant_height), soil_ph=VALUES(soil_ph), soil_moisture=VALUES(soil_moisture), review_status='approved', updated_at=NOW(), updated_by=@teacher_id, remark=VALUES(remark);

INSERT INTO sys_file_resource
(file_no,file_name,original_filename,file_type,file_format,file_size,file_url,storage_path,storage_type,access_level,content_type,uploader_id,uploader_name,uploaded_at,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
VALUES
('DEMO_AGENT_FILE_WHOLE_01','whole-stage-01.png','黄连整株演示样本01.png','image','png',NULL,'pending','demo-agent/whole-stage-01.png','local','private','image/png',@collector_id,'科研 Agent 演示采集员',NOW(),1,0,NOW(),NOW(),@collector_id,@collector_id,0,'科研演示证据样本，非现场实测'),
('DEMO_AGENT_FILE_LEAF_02','leaf-stage-02.png','黄连叶片演示样本02.png','image','png',NULL,'pending','demo-agent/leaf-stage-02.png','local','private','image/png',@collector_id,'科研 Agent 演示采集员',NOW(),1,0,NOW(),NOW(),@collector_id,@collector_id,0,'科研演示证据样本，非现场实测'),
('DEMO_AGENT_FILE_WHOLE_03','whole-stage-03.jpeg','黄连整株演示样本03.jpeg','image','jpeg',NULL,'pending','demo-agent/whole-stage-03.jpeg','local','private','image/jpeg',@collector_id,'科研 Agent 演示采集员',NOW(),1,0,NOW(),NOW(),@collector_id,@collector_id,0,'科研演示证据样本，非现场实测')
ON DUPLICATE KEY UPDATE storage_path=VALUES(storage_path), status=1, is_deleted=0, updated_at=NOW(), remark=VALUES(remark);
UPDATE sys_file_resource SET file_url=CONCAT('/api/files/',id,'/content') WHERE file_no IN ('DEMO_AGENT_FILE_WHOLE_01','DEMO_AGENT_FILE_LEAF_02','DEMO_AGENT_FILE_WHOLE_03');

SET @file1 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_WHOLE_01' LIMIT 1);
SET @file2 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_LEAF_02' LIMIT 1);
SET @file3 := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_WHOLE_03' LIMIT 1);
SET @record1 := (SELECT id FROM herb_growth_record WHERE batch_id=@batch1 LIMIT 1);
SET @record2 := (SELECT id FROM herb_growth_record WHERE batch_id=@batch2 LIMIT 1);
SET @record3 := (SELECT id FROM herb_growth_record WHERE batch_id=@batch3 LIMIT 1);

INSERT INTO herb_image
(image_no,species_id,growth_record_id,image_url,original_filename,file_format,image_type,image_purpose,upload_source,uploader_id,collected_location,longitude,latitude,collected_at,growth_stage,process_status,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT 'DEMO_AGENT_IMG_WHOLE_01',@species_id,@record1,CONCAT('/api/files/',@file1,'/content'),'黄连整株演示样本01.png','png','whole_plant','growth_record','demo_seed',@collector_id,'科研 Agent 演示样方',108.2450000,30.1840000,'2026-06-01 09:25:00','返青期','success',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'演示图片样本，非现场实测'
ON DUPLICATE KEY UPDATE growth_record_id=VALUES(growth_record_id), image_url=VALUES(image_url), status=1, is_deleted=0, updated_at=NOW();
INSERT INTO herb_image
(image_no,species_id,growth_record_id,image_url,original_filename,file_format,image_type,image_purpose,upload_source,uploader_id,collected_location,longitude,latitude,collected_at,growth_stage,process_status,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT 'DEMO_AGENT_IMG_LEAF_02',@species_id,@record2,CONCAT('/api/files/',@file2,'/content'),'黄连叶片演示样本02.png','png','leaf','growth_record','demo_seed',@collector_id,'科研 Agent 演示样方',108.2450000,30.1840000,'2026-06-18 09:25:00','展叶期','success',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'演示图片样本，非现场实测'
ON DUPLICATE KEY UPDATE growth_record_id=VALUES(growth_record_id), image_url=VALUES(image_url), status=1, is_deleted=0, updated_at=NOW();
INSERT INTO herb_image
(image_no,species_id,growth_record_id,image_url,original_filename,file_format,image_type,image_purpose,upload_source,uploader_id,collected_location,longitude,latitude,collected_at,growth_stage,process_status,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT 'DEMO_AGENT_IMG_WHOLE_03',@species_id,@record3,CONCAT('/api/files/',@file3,'/content'),'黄连整株演示样本03.jpeg','jpeg','whole_plant','growth_record','demo_seed',@collector_id,'科研 Agent 演示样方',108.2450000,30.1840000,'2026-07-05 09:25:00','旺盛生长期','success',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'演示图片样本，非现场实测'
ON DUPLICATE KEY UPDATE growth_record_id=VALUES(growth_record_id), image_url=VALUES(image_url), status=1, is_deleted=0, updated_at=NOW();

SET @img1 := (SELECT id FROM herb_image WHERE image_no='DEMO_AGENT_IMG_WHOLE_01' LIMIT 1);
SET @img2 := (SELECT id FROM herb_image WHERE image_no='DEMO_AGENT_IMG_LEAF_02' LIMIT 1);
SET @img3 := (SELECT id FROM herb_image WHERE image_no='DEMO_AGENT_IMG_WHOLE_03' LIMIT 1);
INSERT INTO herb_batch_image (batch_id,image_id,image_role,is_primary,bind_status,sort_order,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @batch1,@img1,'whole_plant',1,'bound',1,1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE NOT EXISTS (SELECT 1 FROM herb_batch_image WHERE batch_id=@batch1 AND image_id=@img1 AND is_deleted=0);
INSERT INTO herb_batch_image (batch_id,image_id,image_role,is_primary,bind_status,sort_order,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @batch2,@img2,'leaf',1,'bound',1,1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE NOT EXISTS (SELECT 1 FROM herb_batch_image WHERE batch_id=@batch2 AND image_id=@img2 AND is_deleted=0);
INSERT INTO herb_batch_image (batch_id,image_id,image_role,is_primary,bind_status,sort_order,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @batch3,@img3,'whole_plant',1,'bound',1,1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE NOT EXISTS (SELECT 1 FROM herb_batch_image WHERE batch_id=@batch3 AND image_id=@img3 AND is_deleted=0);

INSERT INTO herb_identification_result
(image_id,final_species_id,final_species_name,final_confidence,result_source,match_result,need_review,review_status,suggestion,identify_time,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @img1,@species_id,'黄连',0.9400,'local_match','matched',0,'confirmed','演示识别结果。','2026-06-01 09:30:00',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE NOT EXISTS (SELECT 1 FROM herb_identification_result WHERE image_id=@img1 AND is_deleted=0);
INSERT INTO herb_identification_result
(image_id,final_species_id,final_species_name,final_confidence,result_source,match_result,need_review,review_status,suggestion,identify_time,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @img2,@species_id,'黄连',0.9100,'local_match','matched',0,'confirmed','演示识别结果。','2026-06-18 09:30:00',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE NOT EXISTS (SELECT 1 FROM herb_identification_result WHERE image_id=@img2 AND is_deleted=0);
INSERT INTO herb_identification_result
(image_id,final_species_id,final_species_name,final_confidence,result_source,match_result,need_review,review_status,suggestion,identify_time,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @img3,@species_id,'黄连',0.6200,'local_match','ambiguous',1,'pending','置信度偏低，建议补拍根茎、叶片和整株图片后复核。','2026-07-05 09:30:00',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE NOT EXISTS (SELECT 1 FROM herb_identification_result WHERE image_id=@img3 AND is_deleted=0);

SELECT @task_id AS demo_collection_task_id, 'DEMO_AGENT_HL_CONTINUOUS' AS task_code;
