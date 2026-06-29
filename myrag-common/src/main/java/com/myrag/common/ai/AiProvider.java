package com.myrag.common.ai;

import java.util.Locale;

public enum AiProvider {

    DASHSCOPE("dashscope"),
    ZHIPUAI("zhipuai");

    private final String id;

    AiProvider(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static AiProvider from(String value) {
        if (value == null || value.isBlank()) {
            return DASHSCOPE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "zhipuai", "zhipu" -> ZHIPUAI;
            default -> DASHSCOPE;
        };
    }

    public int defaultEmbeddingDimensions() {
        return this == ZHIPUAI ? 1536 : 1024;
    }

    public String defaultRouterModel() {
        return this == ZHIPUAI ? "glm-4-flash" : "qwen-turbo";
    }

    public String defaultChatModel() {
        return this == ZHIPUAI ? "glm-4-plus" : "qwen-plus";
    }

    public String defaultEmbeddingModel() {
        return this == ZHIPUAI ? "embedding-3" : "text-embedding-v3";
    }

    public String defaultBaseUrl() {
        return this == ZHIPUAI ? "https://open.bigmodel.cn/api/paas" : null;
    }
}
