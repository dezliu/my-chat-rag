CREATE TABLE chat_cache_log (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    query TEXT,
    hit BOOLEAN NOT NULL,
    used_rag BOOLEAN,
    kb_ids TEXT,
    latency_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_chat_cache_log_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
