-- BDIS 答辩演示附件绑定（本地开发库专用）。
-- 由 scripts/prepare-defense-demo.mjs 在六个素材文件齐全且已复制到存储目录后执行。
-- 不得移入 Flyway migration 目录。

SET NAMES utf8mb4;
START TRANSACTION;

SET @teacher_id := (SELECT id FROM sys_user WHERE username='agent_teacher_demo' AND is_deleted=0 LIMIT 1);
SET @student_id := (SELECT id FROM sys_user WHERE username='student_hl_demo' AND is_deleted=0 LIMIT 1);
SET @researcher_id := (SELECT id FROM sys_user WHERE username='researcher_hl_demo' AND is_deleted=0 LIMIT 1);
SET @reviewer_id := (SELECT id FROM sys_user WHERE username='agent_reviewer_demo' AND is_deleted=0 LIMIT 1);
SET @course_id := (SELECT id FROM edu_course WHERE course_no='DEMO-HL-COURSE-01' AND is_deleted=0 LIMIT 1);
SET @experiment_id := (SELECT id FROM edu_experiment_record WHERE record_no='DEMO-HL-EXP-01' AND is_deleted=0 LIMIT 1);
SET @project_id := (SELECT id FROM research_project WHERE project_no='DEMO-HL-RESEARCH-01' AND is_deleted=0 LIMIT 1);
SET @declaration_pending_id := (SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-PENDING' AND is_deleted=0 LIMIT 1);
SET @declaration_approved_id := (SELECT id FROM eval_application WHERE application_no='DEMO-HL-DECL-APPROVED' AND is_deleted=0 LIMIT 1);
SET @declaration_archive_id := (SELECT id FROM eval_archive WHERE application_id=@declaration_approved_id AND is_deleted=0 LIMIT 1);
SET @performance_pending_id := (SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-PENDING' AND is_deleted=0 LIMIT 1);
SET @performance_approved_id := (SELECT id FROM perf_record WHERE performance_no='DEMO-HL-PERF-APPROVED' AND is_deleted=0 LIMIT 1);

INSERT INTO sys_file_resource
(file_no, file_name, original_filename, file_type, file_format, file_size,
 file_url, storage_path, storage_type, access_level, content_type,
 uploader_id, uploader_name, uploaded_at, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
VALUES
('DEMO_HL_ASSET_OBSERVATION_FORM', 'huanglian-observation-form.pdf', '黄连生长观测记录表.pdf', 'document', 'pdf', NULL, 'pending', 'defense-demo/huanglian-observation-form.pdf', 'local', 'private', 'application/pdf', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S03'),
('DEMO_HL_ASSET_IDENTIFICATION_VIDEO', 'huanglian-identification-demo.mp4', '黄连形态识别演示.mp4', 'video', 'mp4', NULL, 'pending', 'defense-demo/huanglian-identification-demo.mp4', 'local', 'private', 'video/mp4', @teacher_id, '黄连演示教师', NOW(), 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S03'),
('DEMO_HL_ASSET_EXPERIMENT_REPORT', 'huanglian-experiment-report.pdf', '黄连三阶段观察实验报告.pdf', 'document', 'pdf', NULL, 'pending', 'defense-demo/huanglian-experiment-report.pdf', 'local', 'private', 'application/pdf', @student_id, '黄连演示学生', NOW(), 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S04'),
('DEMO_HL_ASSET_STAGE_COMPARISON', 'huanglian-stage-comparison.png', '黄连阶段影像对比.png', 'image', 'png', NULL, 'pending', 'defense-demo/huanglian-stage-comparison.png', 'local', 'private', 'image/png', @researcher_id, '黄连演示研究员', NOW(), 1, 0, NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S07'),
('DEMO_HL_ASSET_RESEARCH_SUMMARY', 'huanglian-research-summary.pdf', '黄连数字化研究过程摘要.pdf', 'document', 'pdf', NULL, 'pending', 'defense-demo/huanglian-research-summary.pdf', 'local', 'private', 'application/pdf', @researcher_id, '黄连演示研究员', NOW(), 1, 0, NOW(), NOW(), @researcher_id, @researcher_id, 0, 'BDIS_DEFENSE_DEMO:S07'),
('DEMO_HL_ASSET_ARCHIVE_SUMMARY', 'huanglian-digital-archive-summary.pdf', '黄连全生命周期数字档案摘要.pdf', 'document', 'pdf', NULL, 'pending', 'defense-demo/huanglian-digital-archive-summary.pdf', 'local', 'private', 'application/pdf', @student_id, '黄连演示学生', NOW(), 1, 0, NOW(), NOW(), @student_id, @student_id, 0, 'BDIS_DEFENSE_DEMO:S18-S19')
ON DUPLICATE KEY UPDATE
 file_name=VALUES(file_name), original_filename=VALUES(original_filename),
 file_type=VALUES(file_type), file_format=VALUES(file_format), storage_path=VALUES(storage_path),
 storage_type='local', access_level='private', content_type=VALUES(content_type),
 uploader_id=VALUES(uploader_id), uploader_name=VALUES(uploader_name),
 status=1, is_deleted=0, updated_at=NOW(), updated_by=VALUES(updated_by), remark=VALUES(remark);

UPDATE sys_file_resource
SET file_url=CONCAT('/api/files/',id,'/content')
WHERE file_no IN (
 'DEMO_HL_ASSET_OBSERVATION_FORM', 'DEMO_HL_ASSET_IDENTIFICATION_VIDEO',
 'DEMO_HL_ASSET_EXPERIMENT_REPORT', 'DEMO_HL_ASSET_STAGE_COMPARISON',
 'DEMO_HL_ASSET_RESEARCH_SUMMARY', 'DEMO_HL_ASSET_ARCHIVE_SUMMARY'
);

SET @file_observation_form := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_ASSET_OBSERVATION_FORM' LIMIT 1);
SET @file_identification_video := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_ASSET_IDENTIFICATION_VIDEO' LIMIT 1);
SET @file_experiment_report := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_ASSET_EXPERIMENT_REPORT' LIMIT 1);
SET @file_stage_comparison := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_ASSET_STAGE_COMPARISON' LIMIT 1);
SET @file_research_summary := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_ASSET_RESEARCH_SUMMARY' LIMIT 1);
SET @file_archive_summary := (SELECT id FROM sys_file_resource WHERE file_no='DEMO_HL_ASSET_ARCHIVE_SUMMARY' LIMIT 1);

-- 课程资源：一份 PDF 和一份真实 MP4，不使用图片伪装视频。
INSERT INTO edu_course_resource
(course_id, resource_name, resource_type, file_id, file_url, file_size, file_format,
 uploader_id, uploaded_at, download_count, sort_order, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @course_id, '黄连生长观测记录表', 'document', f.id, f.file_url, f.file_size, f.file_format,
 @teacher_id, '2026-03-01 09:00:00', 3, 1, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S03'
FROM sys_file_resource f WHERE f.id=@file_observation_form
  AND NOT EXISTS (SELECT 1 FROM edu_course_resource WHERE course_id=@course_id AND file_id=f.id AND is_deleted=0);
INSERT INTO edu_course_resource
(course_id, resource_name, resource_type, file_id, file_url, file_size, file_format,
 uploader_id, uploaded_at, download_count, sort_order, status, is_deleted,
 created_at, updated_at, created_by, updated_by, version, remark)
SELECT @course_id, '黄连形态识别演示', 'video', f.id, f.file_url, f.file_size, f.file_format,
 @teacher_id, '2026-03-01 09:10:00', 6, 2, 1, 0, NOW(), NOW(), @teacher_id, @teacher_id, 0, 'BDIS_DEFENSE_DEMO:S03'
FROM sys_file_resource f WHERE f.id=@file_identification_video
  AND NOT EXISTS (SELECT 1 FROM edu_course_resource WHERE course_id=@course_id AND file_id=f.id AND is_deleted=0);

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@file_observation_form, 'edu_course', @course_id, 'document', 0, 1, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO:S03'),
(@file_identification_video, 'edu_course', @course_id, 'video', 0, 2, NOW(), @teacher_id, 'BDIS_DEFENSE_DEMO:S03');

UPDATE edu_course
SET video_url=CONCAT('/api/files/',@file_identification_video,'/content'), updated_at=NOW(), updated_by=@teacher_id
WHERE id=@course_id;

-- 学生实验报告与提交版本。
UPDATE edu_experiment_record
SET report_file_id=@file_experiment_report, updated_at=NOW(), updated_by=@teacher_id
WHERE id=@experiment_id;

INSERT INTO edu_experiment_record_version
(record_id, version_no, report_file_id, experiment_title, experiment_process,
 experiment_result, submitted_by, submitted_at, status, created_at, updated_at, version)
SELECT r.id, 1, @file_experiment_report, r.experiment_title, r.experiment_process,
 r.experiment_result, @student_id, '2026-07-08 15:00:00', 'submitted', NOW(), NOW(), 0
FROM edu_experiment_record r
WHERE r.id=@experiment_id
  AND NOT EXISTS (SELECT 1 FROM edu_experiment_record_version v WHERE v.record_id=r.id AND v.version_no=1);
UPDATE edu_experiment_record_version
SET report_file_id=@file_experiment_report, updated_at=NOW()
WHERE record_id=@experiment_id AND version_no=1;

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@file_experiment_report, 'edu_experiment_record', @experiment_id, 'report', 0, 1, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:S04');

-- 课题过程材料：删除两条临时图片绑定，替换为阶段对比图和研究摘要。
DELETE FROM sys_file_business
WHERE biz_type='research_project' AND biz_id=@project_id
  AND remark='BDIS_DEFENSE_DEMO:FALLBACK:S07';
INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@file_stage_comparison, 'research_project', @project_id, 'image', 0, 1, NOW(), @researcher_id, 'BDIS_DEFENSE_DEMO:S07'),
(@file_research_summary, 'research_project', @project_id, 'report', 0, 2, NOW(), @researcher_id, 'BDIS_DEFENSE_DEMO:S07');

-- 替换两条申报中的临时黄连图片材料。
DELETE i FROM eval_archive_item i
JOIN eval_attachment a ON a.id=i.source_id AND i.source_type='attachment'
WHERE i.archive_id=@declaration_archive_id
  AND a.remark IN ('BDIS_DEFENSE_DEMO:FALLBACK:S16');
DELETE FROM sys_file_business
WHERE biz_type='eval_application'
  AND biz_id IN (@declaration_pending_id,@declaration_approved_id)
  AND remark IN ('BDIS_DEFENSE_DEMO:FALLBACK:S15','BDIS_DEFENSE_DEMO:FALLBACK:S16');
DELETE FROM eval_attachment
WHERE application_id IN (@declaration_pending_id,@declaration_approved_id)
  AND remark IN ('BDIS_DEFENSE_DEMO:FALLBACK:S15','BDIS_DEFENSE_DEMO:FALLBACK:S16');

INSERT INTO eval_attachment
(application_id, file_id, file_name, file_type, file_url, file_size, uploader_id,
 uploaded_at, status, is_deleted, created_at, updated_at, created_by, updated_by, version, remark)
SELECT d.application_id, f.id, f.original_filename, f.file_type, f.file_url, f.file_size, @student_id,
 d.uploaded_at, 1, 0, NOW(), NOW(), @student_id, @student_id, 0, d.remark
FROM (
 SELECT @declaration_pending_id AS application_id, @file_experiment_report AS file_id, '2026-07-13 08:30:00' AS uploaded_at, 'BDIS_DEFENSE_DEMO:S15' AS remark
 UNION ALL SELECT @declaration_pending_id, @file_stage_comparison, '2026-07-13 08:31:00', 'BDIS_DEFENSE_DEMO:S15'
 UNION ALL SELECT @declaration_pending_id, @file_research_summary, '2026-07-13 08:32:00', 'BDIS_DEFENSE_DEMO:S15'
 UNION ALL SELECT @declaration_approved_id, @file_experiment_report, '2026-07-10 08:30:00', 'BDIS_DEFENSE_DEMO:S16'
 UNION ALL SELECT @declaration_approved_id, @file_stage_comparison, '2026-07-10 08:31:00', 'BDIS_DEFENSE_DEMO:S16'
 UNION ALL SELECT @declaration_approved_id, @file_research_summary, '2026-07-10 08:32:00', 'BDIS_DEFENSE_DEMO:S16'
) d
JOIN sys_file_resource f ON f.id=d.file_id
WHERE NOT EXISTS (
 SELECT 1 FROM eval_attachment a
 WHERE a.application_id=d.application_id AND a.file_id=d.file_id AND a.is_deleted=0
);

INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
SELECT d.file_id, 'eval_application', d.application_id, 'application_material', 0, d.sort_order,
 NOW(), @student_id, d.remark
FROM (
 SELECT @declaration_pending_id AS application_id, @file_experiment_report AS file_id, 1 AS sort_order, 'BDIS_DEFENSE_DEMO:S15' AS remark
 UNION ALL SELECT @declaration_pending_id, @file_stage_comparison, 2, 'BDIS_DEFENSE_DEMO:S15'
 UNION ALL SELECT @declaration_pending_id, @file_research_summary, 3, 'BDIS_DEFENSE_DEMO:S15'
 UNION ALL SELECT @declaration_approved_id, @file_experiment_report, 1, 'BDIS_DEFENSE_DEMO:S16'
 UNION ALL SELECT @declaration_approved_id, @file_stage_comparison, 2, 'BDIS_DEFENSE_DEMO:S16'
 UNION ALL SELECT @declaration_approved_id, @file_research_summary, 3, 'BDIS_DEFENSE_DEMO:S16'
) d;

INSERT INTO eval_archive_item
(archive_id, source_type, source_id, item_name, item_desc, sort_order, created_at, created_by, remark)
SELECT @declaration_archive_id, 'attachment', a.id, a.file_name, '申报附件',
 CASE a.file_id WHEN @file_experiment_report THEN 1 WHEN @file_stage_comparison THEN 2 ELSE 3 END,
 NOW(), @reviewer_id, 'BDIS_DEFENSE_DEMO:S16'
FROM eval_attachment a
WHERE a.application_id=@declaration_approved_id AND a.is_deleted=0
  AND a.file_id IN (@file_experiment_report,@file_stage_comparison,@file_research_summary)
  AND NOT EXISTS (
    SELECT 1 FROM eval_archive_item i
    WHERE i.archive_id=@declaration_archive_id AND i.source_type='attachment' AND i.source_id=a.id
  );

-- 业绩材料：用研究过程摘要和数字档案摘要替换临时图片。
DELETE FROM sys_file_business
WHERE biz_type='perf_record'
  AND biz_id IN (@performance_pending_id,@performance_approved_id)
  AND remark IN ('BDIS_DEFENSE_DEMO:FALLBACK:S18','BDIS_DEFENSE_DEMO:FALLBACK:S19');
INSERT IGNORE INTO sys_file_business
(file_id, biz_type, biz_id, file_usage, is_public, sort_order, created_at, created_by, remark)
VALUES
(@file_research_summary, 'perf_record', @performance_pending_id, 'material', 0, 1, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:S18'),
(@file_archive_summary, 'perf_record', @performance_pending_id, 'material', 0, 2, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:S18'),
(@file_research_summary, 'perf_record', @performance_approved_id, 'material', 0, 1, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:S19'),
(@file_archive_summary, 'perf_record', @performance_approved_id, 'material', 0, 2, NOW(), @student_id, 'BDIS_DEFENSE_DEMO:S19');

COMMIT;

SELECT
 @file_observation_form AS observation_form_file_id,
 @file_identification_video AS identification_video_file_id,
 @file_experiment_report AS experiment_report_file_id,
 @file_stage_comparison AS stage_comparison_file_id,
 @file_research_summary AS research_summary_file_id,
 @file_archive_summary AS archive_summary_file_id;

