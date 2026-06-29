CREATE TABLE knowledge_base (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    collection_name VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    chunk_config_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_knowledge_base_collection_name (collection_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    kb_id VARCHAR(36) NOT NULL,
    filename VARCHAR(500) NOT NULL,
    content_hash VARCHAR(64),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    chunk_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id),
    KEY idx_document_kb_id (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_chunk (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    doc_id VARCHAR(36) NOT NULL,
    kb_id VARCHAR(36) NOT NULL,
    chunk_index INT NOT NULL,
    content_preview TEXT,
    qdrant_point_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_chunk_doc FOREIGN KEY (doc_id) REFERENCES document(id) ON DELETE CASCADE,
    KEY idx_document_chunk_doc_id (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recall_log (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    kb_id VARCHAR(36),
    query TEXT,
    result_count INT,
    top_scores_json TEXT,
    latency_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_recall_log_kb_id (kb_id),
    KEY idx_recall_log_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recall_quality_alert (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    recall_log_id VARCHAR(36),
    kb_id VARCHAR(36),
    query TEXT,
    quality_score DOUBLE,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_prompt_config (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    prompt_text TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_session (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chat_message (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    rag_kb_ids TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id),
    KEY idx_chat_message_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
