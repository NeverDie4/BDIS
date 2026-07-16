INSERT INTO `dict_type`
(`type_code`, `type_name`, `sort_order`, `status`, `remark`)
VALUES
('herb_category', '中药材分类', 10, 1, '药材资源分类字典')
ON DUPLICATE KEY UPDATE
    type_name = VALUES(type_name),
    sort_order = VALUES(sort_order),
    status = 1,
    is_deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL,
    remark = VALUES(remark);

INSERT INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`, `parent_id`, `sort_order`, `status`, `remark`)
SELECT
    dt.id,
    seed.item_code,
    seed.item_name,
    seed.item_code,
    0,
    seed.sort_order,
    1,
    '药材资源标准分类'
FROM `dict_type` dt
JOIN (
    SELECT 'HERB_CAT_QINGRE' AS item_code, '清热类' AS item_name, 10 AS sort_order
    UNION ALL
    SELECT 'HERB_CAT_BUYI', '补益类', 20
    UNION ALL
    SELECT 'HERB_CAT_HUOXUE', '活血化瘀类', 30
) seed
WHERE dt.type_code = 'herb_category'
  AND dt.is_deleted = 0
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name),
    item_value = VALUES(item_value),
    sort_order = VALUES(sort_order),
    status = 1,
    is_deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL,
    remark = VALUES(remark);

INSERT INTO `dict_item`
(`type_id`, `item_code`, `item_name`, `item_value`, `parent_id`, `sort_order`, `status`, `remark`)
SELECT
    dt.id,
    existing.category_code,
    existing.category_code,
    existing.category_code,
    0,
    1000,
    1,
    '迁移自已有药材分类编码'
FROM `dict_type` dt
JOIN (
    SELECT DISTINCT category_code
    FROM herb_species
    WHERE is_deleted = 0
      AND category_code IS NOT NULL
      AND TRIM(category_code) <> ''
) existing
LEFT JOIN `dict_item` item
       ON item.type_id = dt.id
      AND item.item_code = existing.category_code
      AND item.is_deleted = 0
WHERE dt.type_code = 'herb_category'
  AND dt.is_deleted = 0
  AND item.id IS NULL
ON DUPLICATE KEY UPDATE
    status = 1,
    is_deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL,
    remark = VALUES(remark);

UPDATE herb_species s
JOIN `dict_type` dt
  ON dt.type_code = 'herb_category'
 AND dt.is_deleted = 0
JOIN `dict_item` item
  ON item.type_id = dt.id
 AND item.item_code = s.category_code
 AND item.is_deleted = 0
SET s.category_id = item.id
WHERE s.is_deleted = 0
  AND s.category_code IS NOT NULL
  AND TRIM(s.category_code) <> ''
  AND (s.category_id IS NULL OR s.category_id <> item.id);
