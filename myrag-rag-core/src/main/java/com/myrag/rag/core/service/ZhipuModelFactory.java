package com.myrag.rag.core.service;

import com.myrag.common.exception.MyragException;
import com.myrag.rag.core.service.AiRuntimeConfigService.EffectiveAiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ZhipuModelFactory implements ProviderModelFactory {

    private final AiRuntimeConfigService configService;

    private volatile long cachedVersion = -1;
    private volatile ChatModel routerChatModel;
    private volatile ChatModel chatChatModel;
    private volatile EmbeddingModel embeddingModel;

    @Override
    public ChatModel routerChatModel() {
        ensureCached();
        return routerChatModel;
    }

    @Override
    public ChatModel chatChatModel() {
        ensureCached();
        return chatChatModel;
    }

    @Override
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
        EffectiveAiConfig config = configService.getEffectiveConfig();
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new MyragException(500, "智谱 API Key 未配置，请在管理后台或环境变量 AI_ZHIPUAI_API_KEY 中设置");
        }

        ZhiPuAiApi api = ZhipuModelSupport.buildApi(config);

        routerChatModel = new ZhiPuAiChatModel(
                api,
                ZhipuModelSupport.buildChatOptions(config.routerModel(), 0.1));

        chatChatModel = new ZhiPuAiChatModel(
                api,
                ZhipuModelSupport.buildChatOptions(config.chatModel(), null));

        embeddingModel = ZhipuModelSupport.buildEmbeddingModel(api, config);

        cachedVersion = version;
    }
}
