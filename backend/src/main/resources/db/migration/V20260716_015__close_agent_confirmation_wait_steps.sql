-- Close confirmation wait steps left open by already terminal Agent actions.
UPDATE `assistant_agent_step` AS waiting_step
INNER JOIN `assistant_agent_step` AS generation_step
    ON generation_step.agent_task_id = waiting_step.agent_task_id
   AND generation_step.step_type = 'GENERATE_COLLECTION_PLAN'
   AND waiting_step.step_no = generation_step.step_no + 1
INNER JOIN `assistant_agent_action` AS action_row
    ON action_row.agent_task_id = generation_step.agent_task_id
   AND action_row.step_id = generation_step.id
SET waiting_step.status = CASE
        WHEN action_row.status = 'FAILED' THEN 'FAILED'
        ELSE 'SUCCEEDED'
    END,
    waiting_step.output_summary = CASE action_row.status
        WHEN 'SUCCEEDED' THEN '用户已确认复测采集方案'
        WHEN 'REJECTED' THEN '用户已拒绝本次复测采集方案'
        WHEN 'CANCELLED' THEN '旧复测采集方案已取消'
        ELSE waiting_step.output_summary
    END,
    waiting_step.error_code = CASE
        WHEN action_row.status = 'FAILED' THEN 'ACTION_FAILED'
        ELSE NULL
    END,
    waiting_step.error_message = CASE
        WHEN action_row.status = 'FAILED' THEN action_row.error_message
        ELSE NULL
    END,
    waiting_step.finish_time = COALESCE(
        action_row.executed_time,
        action_row.rejected_time,
        action_row.confirmed_time,
        action_row.update_time
    ),
    waiting_step.update_time = action_row.update_time
WHERE waiting_step.step_type = 'WAIT_FOR_CONFIRMATION'
  AND waiting_step.status = 'WAITING'
  AND action_row.status IN ('SUCCEEDED', 'REJECTED', 'CANCELLED', 'FAILED');

