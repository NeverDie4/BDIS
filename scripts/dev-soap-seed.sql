-- Development-only SOAP demo prerequisites.
--
-- Run only against a local Flyway-migrated development database. This script is intentionally
-- outside backend/src/main/resources/db/migration: demo data must not become production history.
-- It is idempotent and only touches rows carrying the DEV_SOAP_* business codes or the
-- "soap demo seed" marker. It does not insert a growth record; the SOAP integration creates it.

INSERT INTO `herb_species`
(`herb_no`, `herb_name`, `medicinal_part`, `origin_area`, `description`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
VALUES
('DEV_SOAP_HUANGLIAN', 'SOAP演示黄连', '根茎', '重庆市', '校内 SOAP 模拟对接演示药材', 1, 0, NOW(), NOW(), 0, 'soap demo seed')
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
(`base_no`, `base_name`, `base_type`, `address`, `longitude`, `latitude`, `contact_name`, `description`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
VALUES
('DEV_SOAP_CHONGQING_BASE', 'SOAP演示重庆基地', 'research', '重庆市石柱县 SOAP 演示区', 108.1130000, 29.9980000, '校内系统模拟账号', '校内 SOAP 模拟对接演示基地', 1, 0, NOW(), NOW(), 0, 'soap demo seed')
ON DUPLICATE KEY UPDATE
  `base_name` = VALUES(`base_name`),
  `base_type` = VALUES(`base_type`),
  `address` = VALUES(`address`),
  `longitude` = VALUES(`longitude`),
  `latitude` = VALUES(`latitude`),
  `contact_name` = VALUES(`contact_name`),
  `description` = VALUES(`description`),
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW(),
  `remark` = VALUES(`remark`);

SET @soap_demo_species_id := (
  SELECT `id`
  FROM `herb_species`
  WHERE `herb_no` = 'DEV_SOAP_HUANGLIAN' AND `is_deleted` = 0
  LIMIT 1
);

SET @soap_demo_base_id := (
  SELECT `id`
  FROM `herb_base`
  WHERE `base_no` = 'DEV_SOAP_CHONGQING_BASE' AND `is_deleted` = 0
  LIMIT 1
);

UPDATE `herb_distribution`
SET
  `location_name` = 'SOAP 演示黄连采集点',
  `longitude` = 108.1130000,
  `latitude` = 29.9980000,
  `province` = '重庆市',
  `city` = '重庆市',
  `district` = '石柱县',
  `address` = '重庆市石柱县 SOAP 演示区',
  `distribution_type` = 'cultivated',
  `source_type` = 'soap',
  `data_source` = 'local mock campus SOAP',
  `status` = 1,
  `is_deleted` = 0,
  `updated_at` = NOW()
WHERE `species_id` = @soap_demo_species_id
  AND `base_id` = @soap_demo_base_id
  AND `remark` = 'soap demo seed';

INSERT INTO `herb_distribution`
(`species_id`, `base_id`, `location_name`, `longitude`, `latitude`, `province`, `city`, `district`, `address`, `distribution_type`, `source_type`, `data_source`, `status`, `is_deleted`, `created_at`, `updated_at`, `version`, `remark`)
SELECT
  @soap_demo_species_id,
  @soap_demo_base_id,
  'SOAP 演示黄连采集点',
  108.1130000,
  29.9980000,
  '重庆市',
  '重庆市',
  '石柱县',
  '重庆市石柱县 SOAP 演示区',
  'cultivated',
  'soap',
  'local mock campus SOAP',
  1,
  0,
  NOW(),
  NOW(),
  0,
  'soap demo seed'
WHERE @soap_demo_species_id IS NOT NULL
  AND @soap_demo_base_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `herb_distribution`
    WHERE `species_id` = @soap_demo_species_id
      AND `base_id` = @soap_demo_base_id
      AND `remark` = 'soap demo seed'
  );
