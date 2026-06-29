ALTER TABLE ai_runtime_config
    ADD COLUMN provider VARCHAR(32) NOT NULL DEFAULT 'dashscope',
    ADD COLUMN base_url VARCHAR(500) NULL,
    ADD COLUMN embedding_dimensions INT NULL;
