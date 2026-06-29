package com.myrag.rag.core.service;

import com.myrag.common.ai.AiProvider;
import com.myrag.common.ai.DynamicModelProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class DynamicModelProviderRouter implements DynamicModelProvider {

    private final AiRuntimeConfigService configService;
    private final DashScopeModelFactory dashScopeModelFactory;
    private final ZhipuModelFactory zhipuModelFactory;

    @Override
    public ChatModel routerChatModel() {
        return delegate().routerChatModel();
    }

    @Override
    public ChatModel chatChatModel() {
        return delegate().chatChatModel();
    }

    @Override
    public EmbeddingModel embeddingModel() {
        return delegate().embeddingModel();
    }

    private ProviderModelFactory delegate() {
        AiProvider provider = configService.getEffectiveConfig().provider();
        return provider == AiProvider.ZHIPUAI ? zhipuModelFactory : dashScopeModelFactory;
    }
}
