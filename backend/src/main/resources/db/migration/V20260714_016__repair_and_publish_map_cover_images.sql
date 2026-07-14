CREATE TABLE IF NOT EXISTS `map_cover_migration_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `distribution_id` BIGINT NOT NULL COMMENT '地图点位 ID',
  `cover_image_url` VARCHAR(500) NOT NULL COMMENT '迁移前封面 URL',
  `candidate_count` INT NOT NULL DEFAULT 0 COMMENT '满足所有权和类型约束的候选数',
  `review_reason` VARCHAR(64) NOT NULL COMMENT '需要人工复核的原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_map_cover_migration_review_distribution` (`distribution_id`)
) ENGINE=InnoDB COMMENT='地图历史封面迁移人工复核清单';

CREATE TEMPORARY TABLE `tmp_map_cover_candidates` (
  `distribution_id` BIGINT NOT NULL,
  `file_id` BIGINT NOT NULL,
  PRIMARY KEY (`distribution_id`, `file_id`)
);

INSERT INTO `tmp_map_cover_candidates` (`distribution_id`, `file_id`)
SELECT distribution.`id`, file_resource.`id`
FROM `herb_distribution` AS distribution
INNER JOIN `sys_file_resource` AS file_resource
  ON (
    distribution.`cover_image_url` = file_resource.`file_url`
    OR distribution.`cover_image_url` = CONCAT('/api/files/', file_resource.`id`, '/content')
    OR distribution.`cover_image_url` = CONCAT('/api/public-files/', file_resource.`id`, '/content')
    OR distribution.`cover_image_url` REGEXP CONCAT(
      '^https?://[^/]+/api/(public-)?files/', file_resource.`id`, '/content$'
    )
  )
WHERE distribution.`is_deleted` = 0
  AND distribution.`cover_image_url` IS NOT NULL
  AND distribution.`cover_image_url` <> ''
  AND distribution.`created_by` IS NOT NULL
  AND distribution.`created_by` = file_resource.`uploader_id`
  AND file_resource.`is_deleted` = 0
  AND file_resource.`status` = 1
  AND LOWER(file_resource.`file_type`) = 'image'
  AND LOWER(file_resource.`content_type`) IN (
    'image/jpeg', 'image/png', 'image/gif', 'image/bmp', 'image/x-ms-bmp'
  )
  AND LOWER(
    COALESCE(
      NULLIF(file_resource.`file_format`, ''),
      SUBSTRING_INDEX(file_resource.`original_filename`, '.', -1)
    )
  ) IN ('jpg', 'jpeg', 'png', 'gif', 'bmp');

CREATE TEMPORARY TABLE `tmp_unique_map_covers` AS
SELECT candidate.`distribution_id`, MIN(candidate.`file_id`) AS `file_id`
FROM `tmp_map_cover_candidates` AS candidate
GROUP BY candidate.`distribution_id`
HAVING COUNT(*) = 1;

INSERT INTO `sys_file_business` (
  `file_id`, `biz_type`, `biz_id`, `file_usage`, `sort_order`, `is_public`,
  `created_at`, `created_by`, `remark`
)
SELECT
  candidate.`file_id`,
  'map_point',
  candidate.`distribution_id`,
  'cover',
  0,
  1,
  CURRENT_TIMESTAMP,
  distribution.`created_by`,
  '历史地图封面保守绑定迁移'
FROM `tmp_unique_map_covers` AS candidate
INNER JOIN `herb_distribution` AS distribution
  ON distribution.`id` = candidate.`distribution_id`
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_file_business` AS any_binding
    WHERE any_binding.`file_id` = candidate.`file_id`
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_file_business` AS map_binding
    WHERE map_binding.`biz_type` = 'map_point'
      AND map_binding.`biz_id` = candidate.`distribution_id`
      AND map_binding.`file_usage` = 'cover'
  );

UPDATE `sys_file_resource` AS file_resource
INNER JOIN `sys_file_business` AS binding
  ON binding.`file_id` = file_resource.`id`
  AND binding.`biz_type` = 'map_point'
  AND binding.`file_usage` = 'cover'
INNER JOIN `herb_distribution` AS distribution
  ON distribution.`id` = binding.`biz_id`
  AND distribution.`is_deleted` = 0
SET binding.`is_public` = 1,
    file_resource.`access_level` = 'public',
    file_resource.`file_url` = CONCAT(
      '/api/public-files/', file_resource.`id`, '/content'
    ),
    file_resource.`thumbnail_url` = CONCAT(
      '/api/public-files/', file_resource.`id`, '/content'
    ),
    distribution.`cover_image_url` = CONCAT(
      '/api/public-files/', file_resource.`id`, '/content'
    )
WHERE file_resource.`is_deleted` = 0
  AND file_resource.`status` = 1
  AND LOWER(file_resource.`file_type`) = 'image'
  AND LOWER(file_resource.`content_type`) IN (
    'image/jpeg', 'image/png', 'image/gif', 'image/bmp', 'image/x-ms-bmp'
  )
  AND LOWER(
    COALESCE(
      NULLIF(file_resource.`file_format`, ''),
      SUBSTRING_INDEX(file_resource.`original_filename`, '.', -1)
    )
  ) IN ('jpg', 'jpeg', 'png', 'gif', 'bmp')
  AND file_resource.`uploader_id` = COALESCE(binding.`created_by`, distribution.`created_by`);

INSERT INTO `map_cover_migration_review` (
  `distribution_id`, `cover_image_url`, `candidate_count`, `review_reason`
)
SELECT
  distribution.`id`,
  distribution.`cover_image_url`,
  COUNT(DISTINCT candidate.`file_id`),
  CASE
    WHEN COUNT(DISTINCT candidate.`file_id`) = 0 THEN 'NO_VERIFIED_CANDIDATE'
    WHEN COUNT(DISTINCT candidate.`file_id`) > 1 THEN 'AMBIGUOUS_CANDIDATES'
    ELSE 'FILE_ALREADY_BOUND'
  END
FROM `herb_distribution` AS distribution
LEFT JOIN `tmp_map_cover_candidates` AS candidate
  ON candidate.`distribution_id` = distribution.`id`
WHERE distribution.`is_deleted` = 0
  AND distribution.`cover_image_url` IS NOT NULL
  AND distribution.`cover_image_url` <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_file_business` AS binding
    WHERE binding.`biz_type` = 'map_point'
      AND binding.`biz_id` = distribution.`id`
      AND binding.`file_usage` = 'cover'
  )
GROUP BY distribution.`id`, distribution.`cover_image_url`
ON DUPLICATE KEY UPDATE
  `cover_image_url` = VALUES(`cover_image_url`),
  `candidate_count` = VALUES(`candidate_count`),
  `review_reason` = VALUES(`review_reason`);

DROP TEMPORARY TABLE `tmp_unique_map_covers`;
DROP TEMPORARY TABLE `tmp_map_cover_candidates`;
