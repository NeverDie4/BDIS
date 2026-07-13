-- M14 experiment-record permission route metadata.
-- Only binds existing permissions; it does not create permissions or grant roles.

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records', `request_method` = 'GET'
WHERE `permission_code` = 'edu:experiment-record:list';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}', `request_method` = 'GET'
WHERE `permission_code` = 'edu:experiment-record:detail';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records', `request_method` = 'POST'
WHERE `permission_code` = 'edu:experiment-record:add';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}', `request_method` = 'PUT'
WHERE `permission_code` = 'edu:experiment-record:update';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}', `request_method` = 'DELETE'
WHERE `permission_code` = 'edu:experiment-record:delete';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}/submit', `request_method` = 'POST'
WHERE `permission_code` = 'edu:experiment-record:submit';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}/archive', `request_method` = 'POST'
WHERE `permission_code` = 'edu:experiment-record:archive';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}/attachments', `request_method` = 'GET'
WHERE `permission_code` = 'edu:experiment-attachment:list';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}/attachments', `request_method` = 'POST'
WHERE `permission_code` = 'edu:experiment-attachment:add';

UPDATE `auth_permission`
SET `api_path` = '/api/experiment-records/{id}/attachments/{fileId}', `request_method` = 'DELETE'
WHERE `permission_code` = 'edu:experiment-attachment:delete';
