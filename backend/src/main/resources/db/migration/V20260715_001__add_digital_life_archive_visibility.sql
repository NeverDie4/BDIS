ALTER TABLE herb_collection_task
  ADD COLUMN trace_code VARCHAR(100) NULL DEFAULT NULL COMMENT '数字生命档案溯源码' AFTER description,
  ADD COLUMN public_visible TINYINT NOT NULL DEFAULT 0 COMMENT '数字生命档案是否公开' AFTER trace_code,
  ADD UNIQUE KEY uk_herb_collection_task_trace_code (trace_code);
