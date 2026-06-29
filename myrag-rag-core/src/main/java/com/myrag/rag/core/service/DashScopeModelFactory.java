package com.myrag.rag.core.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.myrag.common.ai.DynamicModelProvider;
import com.myrag.common.config.HybridSearchProperties;
import com.myrag.common.exception.MyragException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashScopeModelFactory implements DynamicModelProvider {

    private final AiRuntimeConfigService configService;
    private final HybridSearchProperties hybridSearchProperties;

    private volatile long cachedVersion = -1;
    private volatile ChatModel routerChatModel;
    private volatile ChatModel chatChatModel;
    private volatile EmbeddingModel embeddingModel;

    public ChatModel routerChatModel() {
        ensureCached();
        return routerChatModel;
    }

    public ChatModel chatChatModel() {
        ensureCached();
        return chatChatModel;
    }

    public EmbeddingModel embeddingModel() {
        ensureCached();
        return embeddingModel;
    }

    private void ensureCached() {
        long version = configService.getConfigVersion();
        if (cachedVersion == version && routerChatModel != null) {
            return;
        }
        synchronized (this) {
            if (cachedVersion == version && routerChatModel != null) {
                return;
            }
            rebuild(version);
        }
    }

    private void rebuild(long version) {
        AiRuntimeConfigService.EffectiveAiConfig config = configService.getEffectiveConfig();
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new MyragException(500, "DashScope API Key 未配置，请在管理后台或环境变量 AI_DASHSCOPE_API_KEY 中设置");
        }

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(config.apiKey())
                .build();

        routerChatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(config.routerModel())
                        .withTemperature(0.1)
                        .build())
                .build();

        chatChatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(config.chatModel())
                        .build())
                .build();

        embeddingModel = DashScopeEmbeddingModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeEmbeddingOptions.builder()
                        .withModel(config.embeddingModel())
                        .withDimensions(hybridSearchProperties.getEmbeddingDimensions())
                        .build())
                .build();

        cachedVersion = version;
    }
}
