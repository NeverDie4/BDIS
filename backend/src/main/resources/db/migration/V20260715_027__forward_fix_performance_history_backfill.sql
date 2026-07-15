-- Forward fix for M18 historical data created before the complete recognition migration.

UPDATE `perf_record`
SET `occurred_at` = COALESCE(`occurred_at`, `created_at`, CURRENT_TIMESTAMP)
WHERE `occurred_at` IS NULL;

UPDATE `perf_record` record
LEFT JOIN `perf_standard` standard ON standard.id = record.standard_id
SET record.`standard_no_snapshot` = COALESCE(record.`standard_no_snapshot`, standard.`standard_no`),
    record.`standard_version_snapshot` = COALESCE(record.`standard_version_snapshot`, standard.`standard_version`),
    record.`standard_name_snapshot` = COALESCE(record.`standard_name_snapshot`, standard.`standard_name`),
    record.`standard_rule_snapshot` = COALESCE(record.`standard_rule_snapshot`, standard.`score_rule`)
WHERE record.`standard_id` IS NOT NULL;
