-- Sampling weight is not collectable in the current field workflow.
-- Remove it only from Agent plans and wait requirements; keep the business column intact.
UPDATE `assistant_agent_collection_plan`
SET required_metrics_json = JSON_REMOVE(
        required_metrics_json,
        REPLACE(
            JSON_UNQUOTE(
                JSON_SEARCH(
                    required_metrics_json,
                    'one',
                    'sampleWeight',
                    NULL,
                    '$[*].metricCode'
                )
            ),
            '.metricCode',
            ''
        )
    ),
    update_time = CURRENT_TIMESTAMP
WHERE required_metrics_json IS NOT NULL
  AND JSON_SEARCH(
        required_metrics_json,
        'one',
        'sampleWeight',
        NULL,
        '$[*].metricCode'
      ) IS NOT NULL;

UPDATE `assistant_agent_wait_condition`
SET condition_json = JSON_REMOVE(
        condition_json,
        REPLACE(
            JSON_UNQUOTE(
                JSON_SEARCH(
                    condition_json,
                    'one',
                    'sampleWeight',
                    NULL,
                    '$.requiredMetrics[*].code'
                )
            ),
            '.code',
            ''
        )
    ),
    update_time = CURRENT_TIMESTAMP
WHERE condition_json IS NOT NULL
  AND JSON_SEARCH(
        condition_json,
        'one',
        'sampleWeight',
        NULL,
        '$.requiredMetrics[*].code'
      ) IS NOT NULL;

UPDATE `assistant_agent_wait_condition`
SET current_snapshot_json = JSON_REMOVE(
        current_snapshot_json,
        JSON_UNQUOTE(
            JSON_SEARCH(
                current_snapshot_json,
                'one',
                '缺少采样重量',
                NULL,
                '$.missingRequirements[*]'
            )
        )
    ),
    update_time = CURRENT_TIMESTAMP
WHERE current_snapshot_json IS NOT NULL
  AND JSON_SEARCH(
        current_snapshot_json,
        'one',
        '缺少采样重量',
        NULL,
        '$.missingRequirements[*]'
      ) IS NOT NULL;

DELETE FROM `assistant_agent_collection_requirement`
WHERE requirement_type = 'METRIC'
  AND requirement_code = 'sampleWeight';

