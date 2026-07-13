-- Keep applied migrations immutable. This migration moves existing AI tables to the current schema.

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_session' AND column_name = 'deleted'
  ) AND NOT EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_session' AND column_name = 'is_deleted'
  ),
  'ALTER TABLE `herb_ai_chat_session` RENAME COLUMN `deleted` TO `is_deleted`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_message' AND column_name = 'deleted'
  ) AND NOT EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_message' AND column_name = 'is_deleted'
  ),
  'ALTER TABLE `herb_ai_chat_message` RENAME COLUMN `deleted` TO `is_deleted`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  NOT EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_message' AND column_name = 'user_id'
  ),
  'ALTER TABLE `herb_ai_chat_message` ADD COLUMN `user_id` BIGINT NULL AFTER `session_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `herb_ai_chat_message` message
JOIN `herb_ai_chat_session` session ON session.`session_id` = message.`session_id`
SET message.`user_id` = session.`user_id`
WHERE message.`user_id` IS NULL;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'herb_ai_chat_message'
      AND constraint_name = 'fk_ai_chat_message_session'
      AND constraint_type = 'FOREIGN KEY'
  ),
  'ALTER TABLE `herb_ai_chat_message` DROP FOREIGN KEY `fk_ai_chat_message_session`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_session' AND index_name = 'uk_ai_chat_session_id'
  ),
  'ALTER TABLE `herb_ai_chat_session` DROP INDEX `uk_ai_chat_session_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  NOT EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_session' AND index_name = 'uk_ai_chat_session_user_id'
  ),
  'ALTER TABLE `herb_ai_chat_session` ADD UNIQUE KEY `uk_ai_chat_session_user_id` (`user_id`, `session_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_message' AND index_name = 'idx_ai_chat_message_session_time'
  ),
  'ALTER TABLE `herb_ai_chat_message` DROP INDEX `idx_ai_chat_message_session_time`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  NOT EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_chat_message' AND index_name = 'idx_ai_chat_message_user_session_time'
  ),
  'ALTER TABLE `herb_ai_chat_message` ADD KEY `idx_ai_chat_message_user_session_time` (`user_id`, `session_id`, `create_time`, `id`)',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE `herb_ai_chat_message`
  ADD CONSTRAINT `fk_ai_chat_message_session`
  FOREIGN KEY (`user_id`, `session_id`)
  REFERENCES `herb_ai_chat_session` (`user_id`, `session_id`);

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_knowledge_doc' AND column_name = 'deleted'
  ) AND NOT EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_knowledge_doc' AND column_name = 'is_deleted'
  ),
  'ALTER TABLE `herb_ai_knowledge_doc` RENAME COLUMN `deleted` TO `is_deleted`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_knowledge_chunk' AND column_name = 'deleted'
  ) AND NOT EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'herb_ai_knowledge_chunk' AND column_name = 'is_deleted'
  ),
  'ALTER TABLE `herb_ai_knowledge_chunk` RENAME COLUMN `deleted` TO `is_deleted`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
