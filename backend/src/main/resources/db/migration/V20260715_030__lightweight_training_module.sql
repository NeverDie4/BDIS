-- Lightweight training module: read-only announcement style access for
-- teachers/students, administrator-only plan composition and participation
-- management, and indexes for participation/questionnaire statistics.

DELETE rp
FROM `rel_role_permission` rp
JOIN `auth_role` r ON r.id = rp.role_id
JOIN `auth_permission` p ON p.id = rp.permission_id
WHERE r.role_code IN ('TEACHER', 'TRAINER', 'STUDENT', 'RESEARCHER', 'REVIEWER')
  AND p.permission_code IN (
    'edu:training-plan:add',
    'edu:training-plan:update',
    'edu:training-plan:delete',
    'edu:training-plan:publish',
    'edu:training-record:add',
    'edu:training-record:update',
    'edu:training-item:save'
  );

CREATE INDEX `idx_edu_training_record_plan_created`
    ON `edu_training_record` (`plan_id`, `created_at`);

CREATE INDEX `idx_edu_training_feedback_submitted`
    ON `edu_training_feedback` (`submitted_at`, `training_record_id`);

CREATE INDEX `idx_rel_training_plan_course_active`
    ON `rel_training_plan_course` (`plan_id`, `status`, `is_deleted`);

CREATE INDEX `idx_rel_training_plan_project_active`
    ON `rel_training_plan_project` (`plan_id`, `status`, `is_deleted`);

CREATE INDEX `idx_rel_training_plan_base_active`
    ON `rel_training_plan_base` (`plan_id`, `status`, `is_deleted`);

CREATE INDEX `idx_rel_training_plan_species_active`
    ON `rel_training_plan_species` (`plan_id`, `status`, `is_deleted`);
