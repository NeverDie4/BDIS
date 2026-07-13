UPDATE `sys_file_resource` AS file_resource
INNER JOIN `sys_file_business` AS binding
  ON binding.`file_id` = file_resource.`id`
  AND binding.`biz_type` = 'map_point'
  AND binding.`file_usage` = 'cover'
INNER JOIN `herb_distribution` AS distribution
  ON distribution.`id` = binding.`biz_id`
  AND distribution.`is_deleted` = 0
SET file_resource.`access_level` = 'public',
    distribution.`cover_image_url` = CONCAT(
      '/api/public-files/', file_resource.`id`, '/content'
    )
WHERE file_resource.`is_deleted` = 0
  AND file_resource.`file_type` = 'image'
  AND file_resource.`uploader_id` = binding.`created_by`;
