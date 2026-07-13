ALTER TABLE `herb_growth_record`
  ADD COLUMN `trace_code` VARCHAR(100) NULL DEFAULT NULL COMMENT '溯源码' AFTER `collected_at`,
  ADD COLUMN `trace_qrcode_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '溯源二维码图片地址' AFTER `trace_code`,
  ADD COLUMN `trace_public_url` VARCHAR(255) NULL DEFAULT NULL COMMENT '公开溯源访问地址' AFTER `trace_qrcode_url`,
  ADD COLUMN `public_visible` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开溯源' AFTER `trace_public_url`,
  ADD COLUMN `trace_generated_time` DATETIME NULL DEFAULT NULL COMMENT '溯源码生成时间' AFTER `public_visible`,
  ADD COLUMN `trace_generated_by` BIGINT NULL DEFAULT NULL COMMENT '溯源码生成操作人ID' AFTER `trace_generated_time`,
  ADD COLUMN `trace_generated_by_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '溯源码生成操作人名称' AFTER `trace_generated_by`,
  ADD UNIQUE KEY `uk_growth_record_trace_code` (`trace_code`);
