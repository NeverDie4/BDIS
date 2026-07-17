-- Repair tasks that became eligible after their public stages were approved.
UPDATE herb_collection_task t
JOIN (
    SELECT task_id
    FROM herb_growth_record
    WHERE is_deleted = 0
      AND status = 1
      AND review_status = 'approved'
      AND public_visible = 1
    GROUP BY task_id
    HAVING COUNT(*) >= 2
) eligible ON eligible.task_id = t.id
SET t.trace_code = COALESCE(NULLIF(t.trace_code, ''), CONCAT('DL-TASK-', LPAD(t.id, 8, '0'))),
    t.public_visible = 1,
    t.updated_at = CURRENT_TIMESTAMP
WHERE t.is_deleted = 0
  AND t.status = 1
  AND t.task_status <> 'cancelled'
  AND (t.public_visible <> 1 OR t.trace_code IS NULL OR t.trace_code = '');
