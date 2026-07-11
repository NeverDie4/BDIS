CREATE TABLE `herb_ai_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(64) NOT NULL,
  `session_title` VARCHAR(100) NOT NULL,
  `user_id` BIGINT NULL,
  `source` VARCHAR(20) NULL,
  `last_message` VARCHAR(500) NULL,
  `last_message_time` DATETIME NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'normal',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chat_session_id` (`session_id`),
  KEY `idx_ai_chat_session_user_time` (`user_id`, `last_message_time`),
  KEY `idx_ai_chat_session_source` (`source`),
  CONSTRAINT `chk_ai_chat_session_status` CHECK (`status` IN ('normal', 'deleted'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话表';

CREATE TABLE `herb_ai_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(64) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `content` LONGTEXT NOT NULL,
  `model_name` VARCHAR(100) NULL,
  `token_count` INT NULL,
  `source` VARCHAR(20) NULL,
  `error_message` VARCHAR(500) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_message_session_time` (`session_id`, `create_time`, `id`),
  CONSTRAINT `fk_ai_chat_message_session`
    FOREIGN KEY (`session_id`) REFERENCES `herb_ai_chat_session` (`session_id`),
  CONSTRAINT `chk_ai_chat_message_role`
    CHECK (`role` IN ('user', 'assistant', 'system'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 消息表';
