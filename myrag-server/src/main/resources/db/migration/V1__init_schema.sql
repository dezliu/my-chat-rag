CREATE TABLE knowledge_base (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    collection_name VARCHAR(200) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    chunk_config_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE document (
    id VARCHAR(36) PRIMARY KEY,
    kb_id VARCHAR(36) NOT NULL REFERENCES knowledge_base(id),
    filename VARCHAR(500) NOT NULL,
    content_hash VARCHAR(64),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    chunk_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_kb_id ON document(kb_id);

CREATE TABLE document_chunk (
    id VARCHAR(36) PRIMARY KEY,
    doc_id VARCHAR(36) NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    kb_id VARCHAR(36) NOT NULL,
    chunk_index INT NOT NULL,
    content_preview TEXT,
    qdrant_point_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_chunk_doc_id ON document_chunk(doc_id);

CREATE TABLE recall_log (
    id VARCHAR(36) PRIMARY KEY,
    kb_id VARCHAR(36),
    query TEXT,
    result_count INT,
    top_scores_json TEXT,
    latency_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recall_log_kb_id ON recall_log(kb_id);
CREATE INDEX idx_recall_log_created_at ON recall_log(created_at DESC);

CREATE TABLE recall_quality_alert (
    id VARCHAR(36) PRIMARY KEY,
    recall_log_id VARCHAR(36),
    kb_id VARCHAR(36),
    query TEXT,
    quality_score DOUBLE PRECISION,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_prompt_config (
    id VARCHAR(36) PRIMARY KEY,
    prompt_text TEXT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_session (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_message (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES chat_session(id),
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    rag_kb_ids TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_message_session_id ON chat_message(session_id);
