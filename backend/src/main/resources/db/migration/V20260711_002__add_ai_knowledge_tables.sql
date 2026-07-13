CREATE TABLE `herb_ai_knowledge_doc` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `doc_code` VARCHAR(64) NOT NULL,
  `doc_title` VARCHAR(200) NOT NULL,
  `doc_type` VARCHAR(30) NOT NULL DEFAULT 'other',
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'manual',
  `file_name` VARCHAR(255) NULL,
  `file_url` VARCHAR(500) NULL,
  `content_text` LONGTEXT NOT NULL,
  `summary` VARCHAR(1000) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'enabled',
  `embedding_status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `chunk_count` INT NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_doc_code` (`doc_code`),
  KEY `idx_doc_type` (`doc_type`),
  KEY `idx_status` (`status`),
  KEY `idx_embedding_status` (`embedding_status`),
  CONSTRAINT `chk_ai_knowledge_doc_type`
    CHECK (`doc_type` IN ('system_guide', 'herb_knowledge', 'faq', 'rule', 'report', 'other')),
  CONSTRAINT `chk_ai_knowledge_doc_status`
    CHECK (`status` IN ('enabled', 'disabled')),
  CONSTRAINT `chk_ai_knowledge_doc_embedding_status`
    CHECK (`embedding_status` IN ('pending', 'processing', 'completed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI knowledge document';

CREATE TABLE `herb_ai_knowledge_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `doc_id` BIGINT NOT NULL,
  `chunk_index` INT NOT NULL,
  `chunk_title` VARCHAR(200) NULL,
  `chunk_content` LONGTEXT NOT NULL,
  `content_hash` VARCHAR(64) NOT NULL,
  `token_count` INT NULL,
  `embedding_status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `vector_id` VARCHAR(200) NULL,
  `metadata_json` JSON NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_doc_id` (`doc_id`),
  KEY `idx_content_hash` (`content_hash`),
  KEY `idx_embedding_status` (`embedding_status`),
  KEY `idx_vector_id` (`vector_id`),
  CONSTRAINT `fk_ai_knowledge_chunk_doc`
    FOREIGN KEY (`doc_id`) REFERENCES `herb_ai_knowledge_doc` (`id`),
  CONSTRAINT `chk_ai_knowledge_chunk_embedding_status`
    CHECK (`embedding_status` IN ('pending', 'processing', 'completed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI knowledge document chunk';
