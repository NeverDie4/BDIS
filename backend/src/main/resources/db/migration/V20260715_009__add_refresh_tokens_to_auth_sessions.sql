ALTER TABLE `auth_user_session`
  ADD COLUMN `refresh_token_hash` CHAR(64) NULL DEFAULT NULL COMMENT 'Refresh Token SHA-256 哈希' AFTER `token_jti`,
  ADD COLUMN `refresh_expires_at` DATETIME NULL DEFAULT NULL COMMENT 'Refresh Token 到期时间' AFTER `expires_at`,
  ADD UNIQUE KEY `uk_auth_user_session_refresh_token_hash` (`refresh_token_hash`),
  ADD KEY `idx_auth_user_session_refresh_expires_at` (`refresh_expires_at`);
