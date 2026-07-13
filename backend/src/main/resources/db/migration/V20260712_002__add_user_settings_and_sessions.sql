-- Add personal settings persistence and authoritative authentication sessions.

ALTER TABLE `sys_user`
  ADD COLUMN `password_changed_at` DATETIME NULL DEFAULT NULL COMMENT '最近一次密码变更时间' AFTER `last_login_at`,
  ADD COLUMN `must_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT '是否要求下次登录后修改密码：0 否，1 是' AFTER `password_changed_at`;

CREATE TABLE `sys_user_preference` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `preference_namespace` VARCHAR(50) NOT NULL COMMENT '设置命名空间',
  `preference_data` JSON NOT NULL COMMENT '用户偏好覆盖值',
  `schema_version` INT NOT NULL DEFAULT 1 COMMENT '数据结构版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_preference_user_namespace` (`user_id`, `preference_namespace`),
  KEY `idx_sys_user_preference_namespace` (`preference_namespace`)
) ENGINE=InnoDB COMMENT='用户个人设置偏好表';

CREATE TABLE `auth_user_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '对客户端公开的会话标识',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `token_jti` VARCHAR(64) NOT NULL COMMENT 'JWT 唯一标识',
  `client_type` VARCHAR(30) NOT NULL DEFAULT 'web' COMMENT '客户端类型：web、mobile、unknown',
  `device_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '设备名称',
  `ip_address` VARCHAR(64) NULL DEFAULT NULL COMMENT '客户端 IP',
  `user_agent` VARCHAR(500) NULL DEFAULT NULL COMMENT '客户端 User-Agent',
  `issued_at` DATETIME NOT NULL COMMENT 'Token 签发时间',
  `last_active_at` DATETIME NOT NULL COMMENT '最近活动时间',
  `expires_at` DATETIME NOT NULL COMMENT 'Token 到期时间',
  `revoked_at` DATETIME NULL DEFAULT NULL COMMENT '撤销时间',
  `revoke_reason` VARCHAR(100) NULL DEFAULT NULL COMMENT '撤销原因',
  `session_status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '会话状态：active、revoked、expired',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_user_session_session_id` (`session_id`),
  UNIQUE KEY `uk_auth_user_session_token_jti` (`token_jti`),
  KEY `idx_auth_user_session_user_status` (`user_id`, `session_status`, `expires_at`),
  KEY `idx_auth_user_session_last_active_at` (`last_active_at`)
) ENGINE=InnoDB COMMENT='用户认证会话表';
