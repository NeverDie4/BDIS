-- The historical 20260714.017 slot was previously occupied by a different
-- migration in some development databases. Seed the herb category dictionary
-- in a new forward migration so those databases receive the required data.
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
SELECT dt.id, seed.item_code, seed.item_name, seed.item_code, 0, seed.sort_order, 1, '药材资源标准分类'
FROM `dict_type` dt
JOIN (
    SELECT 'HERB_CAT_QINGRE' AS item_code, '清热类' AS item_name, 10 AS sort_order
    UNION ALL SELECT 'HERB_CAT_BUYI', '补益类', 20
    UNION ALL SELECT 'HERB_CAT_HUOXUE', '活血化瘀类', 30
) seed
WHERE dt.type_code = 'herb_category' AND dt.is_deleted = 0
ON DUPLICATE KEY UPDATE
    item_name = VALUES(item_name),
    item_value = VALUES(item_value),
    sort_order = VALUES(sort_order),
    status = 1,
    is_deleted = 0,
    deleted_at = NULL,
    deleted_by = NULL,
    remark = VALUES(remark);
