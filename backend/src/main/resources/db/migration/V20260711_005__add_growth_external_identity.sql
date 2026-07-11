ALTER TABLE `herb_growth_record`
  ADD COLUMN `external_source` VARCHAR(50) NULL DEFAULT NULL COMMENT '外部来源系统' AFTER `data_source`,
  ADD COLUMN `external_no` VARCHAR(100) NULL DEFAULT NULL COMMENT '外部业务编号' AFTER `external_source`,
  ADD UNIQUE KEY `uk_herb_growth_record_external_identity` (`external_source`, `external_no`);
