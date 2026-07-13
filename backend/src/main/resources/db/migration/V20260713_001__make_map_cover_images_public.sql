UPDATE `sys_file_resource` AS file_resource
INNER JOIN `herb_distribution` AS distribution
  ON distribution.`cover_image_url` = file_resource.`file_url`
  OR distribution.`cover_image_url` LIKE CONCAT('%', file_resource.`file_url`)
  OR distribution.`cover_image_url` LIKE CONCAT('%/api/files/', file_resource.`id`, '/content')
SET file_resource.`access_level` = 'public'
WHERE distribution.`cover_image_url` IS NOT NULL
  AND distribution.`cover_image_url` <> '';

UPDATE `herb_distribution` AS distribution
INNER JOIN `sys_file_resource` AS file_resource
  ON distribution.`cover_image_url` = file_resource.`file_url`
  OR distribution.`cover_image_url` LIKE CONCAT('%', file_resource.`file_url`)
  OR distribution.`cover_image_url` LIKE CONCAT('%/api/files/', file_resource.`id`, '/content')
SET distribution.`cover_image_url` =
  CASE
    WHEN distribution.`cover_image_url` LIKE CONCAT('%/api/files/', file_resource.`id`, '/content')
      THEN REPLACE(
        distribution.`cover_image_url`,
        CONCAT('/api/files/', file_resource.`id`, '/content'),
        CONCAT('/api/public-files/', file_resource.`id`, '/content')
      )
    ELSE REPLACE(
      distribution.`cover_image_url`,
      file_resource.`file_url`,
      CONCAT('/api/public-files/', file_resource.`id`, '/content')
    )
  END
WHERE distribution.`cover_image_url` IS NOT NULL
  AND distribution.`cover_image_url` <> '';
