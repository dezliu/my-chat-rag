package com.myrag.rag.core.service;

import com.myrag.common.config.ChatProperties;
import com.myrag.common.dto.AiConfigDto;
import com.myrag.common.dto.AiConfigUpdateRequest;
import com.myrag.common.util.ApiKeyMasker;
import com.myrag.rag.core.entity.AiRuntimeConfigEntity;
import com.myrag.rag.core.repository.AiRuntimeConfigRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiRuntimeConfigService {

    private final AiRuntimeConfigRepository repository;
    private final ChatProperties chatProperties;

    @Value("${spring.ai.dashscope.api-key:}")
    private String envApiKey;

    @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v3}")
    private String defaultEmbeddingModel;

    @Getter
    private volatile long configVersion = 0;

    public EffectiveAiConfig getEffectiveConfig() {
        AiRuntimeConfigEntity entity = findEntity();
        String dbApiKey = entity != null ? entity.getApiKey() : null;
        boolean useDbKey = dbApiKey != null && !dbApiKey.isBlank();
        String effectiveApiKey = useDbKey ? dbApiKey : envApiKey;

        return new EffectiveAiConfig(
                effectiveApiKey,
                useDbKey ? "db" : "env",
                resolveModel(entity != null ? entity.getRouterModel() : null, chatProperties.getRouterModel()),
                resolveModel(entity != null ? entity.getChatModel() : null, chatProperties.getChatModel()),
                resolveModel(entity != null ? entity.getEmbeddingModel() : null, defaultEmbeddingModel)
        );
    }

    public AiConfigDto toAdminDto() {
        EffectiveAiConfig config = getEffectiveConfig();
        AiRuntimeConfigEntity entity = findEntity();
        String dbApiKey = entity != null ? entity.getApiKey() : null;
        boolean configured = config.apiKey() != null && !config.apiKey().isBlank();

        return AiConfigDto.builder()
                .apiKeyMasked(ApiKeyMasker.mask(configured ? config.apiKey() : ""))
                .apiKeyConfigured(configured)
                .apiKeySource(config.apiKeySource())
                .routerModel(config.routerModel())
                .chatModel(config.chatModel())
                .embeddingModel(config.embeddingModel())
                .build();
    }

    @Transactional
    public AiConfigDto update(AiConfigUpdateRequest request) {
        AiRuntimeConfigEntity entity = repository.findById(AiRuntimeConfigEntity.DEFAULT_ID)
                .orElseGet(() -> AiRuntimeConfigEntity.builder()
                        .id(AiRuntimeConfigEntity.DEFAULT_ID)
                        .build());

        if (request.getRouterModel() != null && !request.getRouterModel().isBlank()) {
            entity.setRouterModel(request.getRouterModel().trim());
        }
        if (request.getChatModel() != null && !request.getChatModel().isBlank()) {
            entity.setChatModel(request.getChatModel().trim());
        }
        if (request.getEmbeddingModel() != null && !request.getEmbeddingModel().isBlank()) {
            entity.setEmbeddingModel(request.getEmbeddingModel().trim());
        }
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            entity.setApiKey(request.getApiKey().trim());
        }

        repository.save(entity);
        configVersion++;
        return toAdminDto();
    }

    private AiRuntimeConfigEntity findEntity() {
        return repository.findById(AiRuntimeConfigEntity.DEFAULT_ID).orElse(null);
    }

    private String resolveModel(String dbValue, String defaultValue) {
        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue;
        }
        return defaultValue;
    }

    public record EffectiveAiConfig(
            String apiKey,
            String apiKeySource,
            String routerModel,
            String chatModel,
            String embeddingModel
    ) {
    }
}
