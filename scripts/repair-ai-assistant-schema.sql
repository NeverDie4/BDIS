-- Upgrade legacy AI assistant tables without deleting existing chat or knowledge data.
ALTER TABLE herb_ai_chat_session
    CHANGE COLUMN deleted is_deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE herb_ai_chat_message
    ADD COLUMN user_id BIGINT NULL AFTER session_id,
    CHANGE COLUMN deleted is_deleted TINYINT NOT NULL DEFAULT 0;

UPDATE herb_ai_chat_message message
INNER JOIN herb_ai_chat_session session
    ON session.session_id = message.session_id
SET message.user_id = session.user_id
WHERE message.user_id IS NULL;

ALTER TABLE herb_ai_chat_message
    ADD KEY idx_ai_chat_message_user_session_time
        (user_id, session_id, create_time, id);

ALTER TABLE herb_ai_knowledge_doc
    CHANGE COLUMN deleted is_deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE herb_ai_knowledge_chunk
    CHANGE COLUMN deleted is_deleted TINYINT NOT NULL DEFAULT 0;
