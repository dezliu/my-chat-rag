package com.myrag.rag.core.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

final class DashScopeModelSupport {

    private static final String MULTIMODAL_COMPLETIONS_PATH =
            "/api/v1/services/aigc/multimodal-generation/generation";

    private DashScopeModelSupport() {
    }

    static boolean requiresMultimodalEndpoint(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String normalized = model.toLowerCase();
        if (normalized.contains("-vl-") || normalized.startsWith("qwen-vl")) {
            return true;
        }
        if (normalized.startsWith("qwen3.")) {
            return !normalized.contains("coder");
        }
        return false;
    }

    static DashScopeApi buildApi(String apiKey, String model) {
        DashScopeApi.Builder builder = DashScopeApi.builder().apiKey(apiKey);
        if (requiresMultimodalEndpoint(model)) {
            builder.completionsPath(MULTIMODAL_COMPLETIONS_PATH);
        }
        return builder.build();
    }

    static DashScopeChatOptions buildChatOptions(String model, Double temperature) {
        DashScopeChatOptions.DashScopeChatOptionsBuilder builder = DashScopeChatOptions.builder()
                .withModel(model);
        if (temperature != null) {
            builder.withTemperature(temperature);
        }
        if (requiresMultimodalEndpoint(model)) {
            builder.withMultiModel(true);
        }
        return builder.build();
    }
}
