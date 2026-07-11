UPDATE herb_collection_task
SET created_by = collector_id,
    updated_by = COALESCE(updated_by, collector_id)
WHERE created_by IS NULL
  AND collector_id IS NOT NULL;

UPDATE herb_batch batch
LEFT JOIN herb_collection_task task
  ON task.id = batch.task_id
 AND task.is_deleted = 0
SET batch.created_by = COALESCE(batch.created_by, task.collector_id, task.created_by),
    batch.updated_by = COALESCE(
      batch.updated_by,
      batch.created_by,
      task.collector_id,
      task.created_by
    )
WHERE batch.created_by IS NULL
   OR batch.updated_by IS NULL;

UPDATE herb_batch_image batch_image
INNER JOIN herb_batch batch
  ON batch.id = batch_image.batch_id
 AND batch.is_deleted = 0
SET batch_image.created_by = COALESCE(batch_image.created_by, batch.created_by),
    batch_image.updated_by = COALESCE(
      batch_image.updated_by,
      batch_image.created_by,
      batch.created_by
    )
WHERE batch_image.created_by IS NULL
   OR batch_image.updated_by IS NULL;

ALTER TABLE herb_collection_task
  ADD KEY idx_herb_collection_task_created_by (created_by);

ALTER TABLE herb_batch
  ADD KEY idx_herb_batch_created_by (created_by);
