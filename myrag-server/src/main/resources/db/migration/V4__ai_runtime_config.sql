CREATE TABLE ai_runtime_config (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    api_key VARCHAR(500),
    router_model VARCHAR(100),
    chat_model VARCHAR(100),
    embedding_model VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
