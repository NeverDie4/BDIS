CREATE TABLE IF NOT EXISTS `herb_species_cover_migration_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `species_id` BIGINT NOT NULL COMMENT '药材 ID',
  `cover_image_url` VARCHAR(500) NOT NULL COMMENT '迁移前封面 URL',
  `candidate_count` INT NOT NULL DEFAULT 0 COMMENT '满足所有权和类型约束的候选数',
  `review_reason` VARCHAR(64) NOT NULL COMMENT '需要人工复核的原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_species_cover_migration_review_species` (`species_id`)
) ENGINE=InnoDB COMMENT='药材历史封面迁移人工复核清单';

CREATE TEMPORARY TABLE `tmp_herb_species_cover_candidates` (
  `species_id` BIGINT NOT NULL,
  `file_id` BIGINT NOT NULL,
  PRIMARY KEY (`species_id`, `file_id`)
);

INSERT INTO `tmp_herb_species_cover_candidates` (`species_id`, `file_id`)
SELECT species.`id`, file_resource.`id`
FROM `herb_species` AS species
INNER JOIN `sys_file_resource` AS file_resource
  ON (
    species.`cover_image_url` = file_resource.`file_url`
    OR species.`cover_image_url` = CONCAT('/api/files/', file_resource.`id`, '/content')
    OR species.`cover_image_url` = CONCAT('/api/public-files/', file_resource.`id`, '/content')
    OR species.`cover_image_url` REGEXP CONCAT(
      '^https?://[^/]+/api/(public-)?files/', file_resource.`id`, '/content$'
    )
  )
WHERE species.`is_deleted` = 0
  AND species.`cover_image_url` IS NOT NULL
  AND species.`cover_image_url` <> ''
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

CREATE TEMPORARY TABLE `tmp_unique_herb_species_covers` AS
SELECT candidate.`species_id`, MIN(candidate.`file_id`) AS `file_id`
FROM `tmp_herb_species_cover_candidates` AS candidate
GROUP BY candidate.`species_id`
HAVING COUNT(*) = 1;

INSERT INTO `sys_file_business` (
  `file_id`, `biz_type`, `biz_id`, `file_usage`, `sort_order`, `is_public`,
  `created_at`, `created_by`, `remark`
)
SELECT
  candidate.`file_id`,
  'herb_species',
  candidate.`species_id`,
  'cover',
  0,
  1,
  CURRENT_TIMESTAMP,
  COALESCE(species.`created_by`, file_resource.`uploader_id`),
  '历史药材封面保守绑定迁移'
FROM `tmp_unique_herb_species_covers` AS candidate
INNER JOIN `herb_species` AS species
  ON species.`id` = candidate.`species_id`
INNER JOIN `sys_file_resource` AS file_resource
  ON file_resource.`id` = candidate.`file_id`
WHERE NOT EXISTS (
    SELECT 1
    FROM `sys_file_business` AS any_binding
    WHERE any_binding.`file_id` = candidate.`file_id`
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_file_business` AS herb_binding
    WHERE herb_binding.`biz_type` = 'herb_species'
      AND herb_binding.`biz_id` = candidate.`species_id`
      AND herb_binding.`file_usage` = 'cover'
  );

UPDATE `sys_file_resource` AS file_resource
INNER JOIN `sys_file_business` AS binding
  ON binding.`file_id` = file_resource.`id`
  AND binding.`biz_type` = 'herb_species'
  AND binding.`file_usage` = 'cover'
INNER JOIN `herb_species` AS species
  ON species.`id` = binding.`biz_id`
  AND species.`is_deleted` = 0
SET binding.`is_public` = 1,
    file_resource.`access_level` = 'public',
    file_resource.`file_url` = CONCAT(
      '/api/public-files/', file_resource.`id`, '/content'
    ),
    file_resource.`thumbnail_url` = CONCAT(
      '/api/public-files/', file_resource.`id`, '/content'
    ),
    species.`cover_image_url` = CONCAT(
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
  AND file_resource.`uploader_id` = COALESCE(
    binding.`created_by`, species.`created_by`, file_resource.`uploader_id`
  );

INSERT INTO `herb_species_cover_migration_review` (
  `species_id`, `cover_image_url`, `candidate_count`, `review_reason`
)
SELECT
  species.`id`,
  species.`cover_image_url`,
  COUNT(DISTINCT candidate.`file_id`),
  CASE
    WHEN COUNT(DISTINCT candidate.`file_id`) = 0 THEN 'NO_VERIFIED_CANDIDATE'
    WHEN COUNT(DISTINCT candidate.`file_id`) > 1 THEN 'AMBIGUOUS_CANDIDATES'
    ELSE 'FILE_ALREADY_BOUND'
  END
FROM `herb_species` AS species
LEFT JOIN `tmp_herb_species_cover_candidates` AS candidate
  ON candidate.`species_id` = species.`id`
WHERE species.`is_deleted` = 0
  AND species.`cover_image_url` IS NOT NULL
  AND species.`cover_image_url` <> ''
  AND species.`cover_image_url` NOT LIKE '%/public-files/%'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_file_business` AS binding
    WHERE binding.`biz_type` = 'herb_species'
      AND binding.`biz_id` = species.`id`
      AND binding.`file_usage` = 'cover'
  )
GROUP BY species.`id`, species.`cover_image_url`
ON DUPLICATE KEY UPDATE
  `cover_image_url` = VALUES(`cover_image_url`),
  `candidate_count` = VALUES(`candidate_count`),
  `review_reason` = VALUES(`review_reason`);

DROP TEMPORARY TABLE `tmp_unique_herb_species_covers`;
DROP TEMPORARY TABLE `tmp_herb_species_cover_candidates`;
