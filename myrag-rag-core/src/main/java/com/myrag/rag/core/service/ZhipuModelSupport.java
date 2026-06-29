package com.myrag.rag.core.service;

import com.myrag.common.exception.MyragException;
import com.myrag.rag.core.service.AiRuntimeConfigService.EffectiveAiConfig;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingOptions;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;

final class ZhipuModelSupport {

    private ZhipuModelSupport() {
    }

    static ZhiPuAiApi buildApi(EffectiveAiConfig config) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new MyragException(500, "智谱 API Key 未配置，请在管理后台或环境变量 AI_ZHIPUAI_API_KEY 中设置");
        }
        ZhiPuAiApi.Builder builder = ZhiPuAiApi.builder().apiKey(config.apiKey());
        if (config.baseUrl() != null && !config.baseUrl().isBlank()) {
            builder.baseUrl(config.baseUrl());
        }
        return builder.build();
    }

    static ZhiPuAiChatOptions buildChatOptions(String model, Double temperature) {
        ZhiPuAiChatOptions options = ZhiPuAiChatOptions.builder()
                .model(model)
                .build();
        if (temperature != null) {
            options.setTemperature(temperature);
        }
        return options;
    }

    static ZhiPuAiEmbeddingOptions buildEmbeddingOptions(EffectiveAiConfig config) {
        return ZhiPuAiEmbeddingOptions.builder()
                .model(config.embeddingModel())
                .dimensions(config.embeddingDimensions())
                .build();
    }

    static ZhiPuAiEmbeddingModel buildEmbeddingModel(ZhiPuAiApi api, EffectiveAiConfig config) {
        return new ZhiPuAiEmbeddingModel(
                api,
                MetadataMode.EMBED,
                buildEmbeddingOptions(config));
    }
}
