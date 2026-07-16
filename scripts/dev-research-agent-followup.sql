-- 在 Agent 已确认创建真实复测任务后运行；只向该 Agent 创建的复测任务补充演示现场数据。
-- 所有数据均标记为 demo_seed / 非现场实测。
SET @collector_id := (SELECT id FROM sys_user WHERE username='collector_agent_demo' AND is_deleted=0 LIMIT 1);
SET @teacher_id := (SELECT id FROM sys_user WHERE username='agent_teacher_demo' AND is_deleted=0 LIMIT 1);
SET @species_id := (SELECT id FROM herb_species WHERE herb_no='DEMO_AGENT_HUANGLIAN' AND is_deleted=0 LIMIT 1);
SET @base_id := (SELECT id FROM herb_base WHERE base_no='DEMO_AGENT_BASE' AND is_deleted=0 LIMIT 1);
SET @source_task_id := (SELECT id FROM herb_collection_task WHERE task_code='DEMO_AGENT_HL_CONTINUOUS' AND is_deleted=0 LIMIT 1);
SET @followup_task_id := (
  SELECT l.business_id FROM assistant_agent_business_link l
  JOIN assistant_agent_task a ON a.id=l.agent_task_id
  WHERE a.collection_task_id=@source_task_id AND l.relation_type='FOLLOW_UP_COLLECTION_TASK'
  ORDER BY l.id DESC LIMIT 1
);
SET @followup_code := (SELECT task_code FROM herb_collection_task WHERE id=@followup_task_id LIMIT 1);

INSERT INTO herb_batch
(batch_code,batch_name,task_id,species_id,species_name,base_id,base_name,origin_place,collect_start_time,collect_end_time,production_date,batch_status,image_count,identified_count,reviewed_count,need_review_count,evaluation_summary,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT CONCAT('DEMO_AGENT_FOLLOWUP_',@followup_task_id),'黄连科研 Agent 复测观测',@followup_task_id,@species_id,'黄连',@base_id,'石柱黄连科研 Agent 演示基地','科研 Agent 演示样方','2026-07-16 09:00:00','2026-07-16 10:30:00','2026-07-16','completed',3,3,3,0,'已补充株高、土壤湿度、土壤 pH 及整株、叶片、根茎图片。',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'demo_seed; non-field-measured'
WHERE @followup_task_id IS NOT NULL
ON DUPLICATE KEY UPDATE batch_status='completed',evaluation_summary=VALUES(evaluation_summary),status=1,is_deleted=0,updated_at=NOW();
SET @batch_id := (SELECT id FROM herb_batch WHERE batch_code=CONCAT('DEMO_AGENT_FOLLOWUP_',@followup_task_id) LIMIT 1);

INSERT INTO herb_growth_record
(batch_id,task_id,species_id,species_name,base_id,base_name,collector_id,collector_name_snapshot,longitude,latitude,growth_stage,plant_height,stem_diameter,soil_ph,temperature,humidity,soil_moisture,light,leaf_color,flowering_status,growth_evaluation,device_type,data_source,review_status,submitted_at,reviewed_at,collected_at,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @batch_id,@followup_task_id,@species_id,'黄连',@base_id,'石柱黄连科研 Agent 演示基地',@collector_id,'科研 Agent 演示采集员',108.2450000,30.1840000,'复测期',17.60,3.40,6.15,23.80,68.00,39.00,14800,'深绿','未开花','复测字段已按 Agent 清单补充；仅描述演示数据事实。','app','demo_seed','approved','2026-07-16 10:35:00','2026-07-16 11:30:00','2026-07-16 09:20:00',1,0,NOW(),NOW(),@collector_id,@teacher_id,0,'DEMO_COORDINATE; not field-measured'
WHERE @batch_id IS NOT NULL
ON DUPLICATE KEY UPDATE plant_height=VALUES(plant_height),soil_ph=VALUES(soil_ph),soil_moisture=VALUES(soil_moisture),review_status='approved',updated_at=NOW(),updated_by=@teacher_id,remark=VALUES(remark);
SET @record_id := (SELECT id FROM herb_growth_record WHERE batch_id=@batch_id LIMIT 1);

INSERT INTO sys_file_resource
(file_no,file_name,original_filename,file_type,file_format,file_size,file_url,storage_path,storage_type,access_level,content_type,uploader_id,uploader_name,uploaded_at,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
VALUES
('DEMO_AGENT_FILE_FU_WHOLE','followup-whole.jpeg','复测整株演示样本.jpeg','image','jpeg',NULL,'pending','demo-agent/followup-whole.jpeg','local','private','image/jpeg',@collector_id,'科研 Agent 演示采集员',NOW(),1,0,NOW(),NOW(),@collector_id,@collector_id,0,'科研演示证据样本，非现场实测'),
('DEMO_AGENT_FILE_FU_LEAF','followup-leaf.jpeg','复测叶片演示样本.jpeg','image','jpeg',NULL,'pending','demo-agent/followup-leaf.jpeg','local','private','image/jpeg',@collector_id,'科研 Agent 演示采集员',NOW(),1,0,NOW(),NOW(),@collector_id,@collector_id,0,'科研演示证据样本，非现场实测'),
('DEMO_AGENT_FILE_FU_ROOT','followup-root.png','复测根茎演示样本.png','image','png',NULL,'pending','demo-agent/followup-root.png','local','private','image/png',@collector_id,'科研 Agent 演示采集员',NOW(),1,0,NOW(),NOW(),@collector_id,@collector_id,0,'科研演示证据样本，非现场实测')
ON DUPLICATE KEY UPDATE storage_path=VALUES(storage_path),status=1,is_deleted=0,updated_at=NOW(),remark=VALUES(remark);
UPDATE sys_file_resource SET file_url=CONCAT('/api/files/',id,'/content') WHERE file_no LIKE 'DEMO_AGENT_FILE_FU_%';

SET @fu_whole_file := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_FU_WHOLE' LIMIT 1);
SET @fu_leaf_file := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_FU_LEAF' LIMIT 1);
SET @fu_root_file := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_AGENT_FILE_FU_ROOT' LIMIT 1);
INSERT INTO herb_image (image_no,species_id,growth_record_id,image_url,original_filename,file_format,image_type,image_purpose,upload_source,uploader_id,collected_location,longitude,latitude,collected_at,growth_stage,process_status,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT 'DEMO_AGENT_IMG_FU_WHOLE',@species_id,@record_id,CONCAT('/api/files/',@fu_whole_file,'/content'),'复测整株演示样本.jpeg','jpeg','whole_plant','growth_record','demo_seed',@collector_id,'科研 Agent 演示样方',108.2450000,30.1840000,'2026-07-16 09:30:00','复测期','success',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'演示图片样本，非现场实测' WHERE @record_id IS NOT NULL
ON DUPLICATE KEY UPDATE growth_record_id=VALUES(growth_record_id),image_url=VALUES(image_url),status=1,is_deleted=0,updated_at=NOW();
INSERT INTO herb_image (image_no,species_id,growth_record_id,image_url,original_filename,file_format,image_type,image_purpose,upload_source,uploader_id,collected_location,longitude,latitude,collected_at,growth_stage,process_status,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT 'DEMO_AGENT_IMG_FU_LEAF',@species_id,@record_id,CONCAT('/api/files/',@fu_leaf_file,'/content'),'复测叶片演示样本.jpeg','jpeg','leaf','growth_record','demo_seed',@collector_id,'科研 Agent 演示样方',108.2450000,30.1840000,'2026-07-16 09:31:00','复测期','success',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'演示图片样本，非现场实测' WHERE @record_id IS NOT NULL
ON DUPLICATE KEY UPDATE growth_record_id=VALUES(growth_record_id),image_url=VALUES(image_url),status=1,is_deleted=0,updated_at=NOW();
INSERT INTO herb_image (image_no,species_id,growth_record_id,image_url,original_filename,file_format,image_type,image_purpose,upload_source,uploader_id,collected_location,longitude,latitude,collected_at,growth_stage,process_status,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT 'DEMO_AGENT_IMG_FU_ROOT',@species_id,@record_id,CONCAT('/api/files/',@fu_root_file,'/content'),'复测根茎演示样本.png','png','root','growth_record','demo_seed',@collector_id,'科研 Agent 演示样方',108.2450000,30.1840000,'2026-07-16 09:32:00','复测期','success',1,0,NOW(),NOW(),@collector_id,@collector_id,0,'演示图片样本，非现场实测' WHERE @record_id IS NOT NULL
ON DUPLICATE KEY UPDATE growth_record_id=VALUES(growth_record_id),image_url=VALUES(image_url),status=1,is_deleted=0,updated_at=NOW();

SET @fu_whole := (SELECT id FROM herb_image WHERE image_no='DEMO_AGENT_IMG_FU_WHOLE' LIMIT 1);
SET @fu_leaf := (SELECT id FROM herb_image WHERE image_no='DEMO_AGENT_IMG_FU_LEAF' LIMIT 1);
SET @fu_root := (SELECT id FROM herb_image WHERE image_no='DEMO_AGENT_IMG_FU_ROOT' LIMIT 1);
INSERT INTO herb_batch_image (batch_id,image_id,image_role,is_primary,bind_status,sort_order,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @batch_id,@fu_whole,'whole_plant',1,'bound',1,1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE @batch_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM herb_batch_image WHERE batch_id=@batch_id AND image_id=@fu_whole AND is_deleted=0);
INSERT INTO herb_batch_image (batch_id,image_id,image_role,is_primary,bind_status,sort_order,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @batch_id,@fu_leaf,'leaf',0,'bound',2,1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE @batch_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM herb_batch_image WHERE batch_id=@batch_id AND image_id=@fu_leaf AND is_deleted=0);
INSERT INTO herb_batch_image (batch_id,image_id,image_role,is_primary,bind_status,sort_order,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT @batch_id,@fu_root,'root',0,'bound',3,1,0,NOW(),NOW(),@collector_id,@collector_id,0,'research agent demo seed' WHERE @batch_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM herb_batch_image WHERE batch_id=@batch_id AND image_id=@fu_root AND is_deleted=0);

INSERT INTO herb_identification_result (image_id,final_species_id,final_species_name,final_confidence,result_source,match_result,need_review,review_status,suggestion,identify_time,status,is_deleted,created_at,updated_at,created_by,updated_by,version,remark)
SELECT image_id,@species_id,'黄连',0.9300,'manual_review','matched',0,'confirmed','复测演示图片已完成识别复核。',NOW(),1,0,NOW(),NOW(),@teacher_id,@teacher_id,0,'research agent demo seed'
FROM (SELECT @fu_whole image_id UNION ALL SELECT @fu_leaf UNION ALL SELECT @fu_root) images
WHERE image_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM herb_identification_result r WHERE r.image_id=images.image_id AND r.is_deleted=0);

SELECT @followup_task_id AS follow_up_task_id, @followup_code AS follow_up_task_code, @batch_id AS follow_up_batch_id;
