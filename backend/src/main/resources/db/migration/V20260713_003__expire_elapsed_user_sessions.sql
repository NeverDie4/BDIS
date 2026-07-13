UPDATE `auth_user_session`
SET `session_status` = 'expired',
    `updated_at` = NOW()
WHERE `session_status` = 'active'
  AND `expires_at` <= NOW();
