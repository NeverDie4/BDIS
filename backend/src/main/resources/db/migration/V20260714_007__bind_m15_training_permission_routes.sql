-- M15 training permission route metadata.
-- AuthorizationService.requirePermission(permissionCode) is the final business authorization
-- check. api_path/request_method are canonical metadata only when one permission protects
-- multiple routes; shared routes remain protected by their Controller permission checks.
-- This migration only updates existing permissions. It creates no permission and grants no role.

UPDATE `auth_permission`
SET `api_path` = '/api/training-plans', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-plan:list';

UPDATE `auth_permission`
SET `api_path` = '/api/training-plans/{id}', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-plan:detail';

UPDATE `auth_permission`
SET `api_path` = '/api/training-plans', `request_method` = 'POST'
WHERE `permission_code` = 'edu:training-plan:add';

UPDATE `auth_permission`
SET `api_path` = '/api/training-plans/{id}', `request_method` = 'PUT'
WHERE `permission_code` = 'edu:training-plan:update';

UPDATE `auth_permission`
SET `api_path` = '/api/training-plans/{id}', `request_method` = 'DELETE'
WHERE `permission_code` = 'edu:training-plan:delete';

-- Also protects POST /api/training-plans/{id}/close in TrainingPlanController.
UPDATE `auth_permission`
SET `api_path` = '/api/training-plans/{id}/publish', `request_method` = 'POST'
WHERE `permission_code` = 'edu:training-plan:publish';

-- Also protects GET /api/training-plans/{id}/materials.
UPDATE `auth_permission`
SET `api_path` = '/api/training-materials', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-material:list';

UPDATE `auth_permission`
SET `api_path` = '/api/training-materials/{id}', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-material:detail';

UPDATE `auth_permission`
SET `api_path` = '/api/training-materials', `request_method` = 'POST'
WHERE `permission_code` = 'edu:training-material:add';

UPDATE `auth_permission`
SET `api_path` = '/api/training-materials/{id}', `request_method` = 'PUT'
WHERE `permission_code` = 'edu:training-material:update';

UPDATE `auth_permission`
SET `api_path` = '/api/training-materials/{id}', `request_method` = 'DELETE'
WHERE `permission_code` = 'edu:training-material:delete';

-- Also protects DELETE /api/training-plans/{id}/materials/{materialId}.
UPDATE `auth_permission`
SET `api_path` = '/api/training-plans/{id}/materials', `request_method` = 'POST'
WHERE `permission_code` = 'edu:training-material:bind';

UPDATE `auth_permission`
SET `api_path` = '/api/training-records', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-record:list';

UPDATE `auth_permission`
SET `api_path` = '/api/training-records/{id}', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-record:detail';

-- Also protects POST /api/training-plans/{planId}/participants/batch.
UPDATE `auth_permission`
SET `api_path` = '/api/training-records', `request_method` = 'POST'
WHERE `permission_code` = 'edu:training-record:add';

-- Also protects DELETE /api/training-records/{id}.
UPDATE `auth_permission`
SET `api_path` = '/api/training-records/{id}', `request_method` = 'PUT'
WHERE `permission_code` = 'edu:training-record:update';

UPDATE `auth_permission`
SET `api_path` = '/api/training-feedbacks', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-feedback:list';

UPDATE `auth_permission`
SET `api_path` = '/api/training-feedbacks', `request_method` = 'POST'
WHERE `permission_code` = 'edu:training-feedback:add';

UPDATE `auth_permission`
SET `api_path` = '/api/training-feedbacks/{id}', `request_method` = 'PUT'
WHERE `permission_code` = 'edu:training-feedback:update';

UPDATE `auth_permission`
SET `api_path` = '/api/training-plans/{id}/summary', `request_method` = 'GET'
WHERE `permission_code` = 'edu:training-summary:view';
