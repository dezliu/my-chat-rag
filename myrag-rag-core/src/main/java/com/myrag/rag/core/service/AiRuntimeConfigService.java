package com.myrag.rag.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myrag.common.ai.AiProvider;
import com.myrag.common.config.AiProperties;
import com.myrag.common.config.ChatProperties;
import com.myrag.common.dto.AiConfigDto;
import com.myrag.common.dto.AiConfigUpdateRequest;
import com.myrag.common.util.ApiKeyMasker;
import com.myrag.rag.core.entity.AiRuntimeConfigEntity;
import com.myrag.rag.core.repository.AiRuntimeConfigRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRuntimeConfigService {

    private final AiRuntimeConfigRepository repository;
    private final ChatProperties chatProperties;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String envDashScopeApiKey;

    @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v3}")
    private String defaultDashScopeEmbeddingModel;

    @Getter
    private volatile long configVersion = 0;

    public EffectiveAiConfig getEffectiveConfig() {
        AiRuntimeConfigEntity entity = findEntity();
        AiProvider provider = resolveProvider(entity);
        String dbApiKey = entity != null ? entity.getApiKey() : null;
        boolean useDbKey = dbApiKey != null && !dbApiKey.isBlank();
        String effectiveApiKey = useDbKey ? dbApiKey : resolveEnvApiKey(provider);
        String baseUrl = resolveBaseUrl(entity, provider);

        return new EffectiveAiConfig(
                provider,
                effectiveApiKey,
                useDbKey ? "db" : "env",
                baseUrl,
                resolveModel(entity != null ? entity.getRouterModel() : null,
                        provider == AiProvider.DASHSCOPE ? chatProperties.getRouterModel() : null,
                        provider.defaultRouterModel()),
                resolveModel(entity != null ? entity.getChatModel() : null,
                        provider == AiProvider.DASHSCOPE ? chatProperties.getChatModel() : null,
                        provider.defaultChatModel()),
                resolveModel(entity != null ? entity.getEmbeddingModel() : null,
                        provider == AiProvider.DASHSCOPE ? defaultDashScopeEmbeddingModel : null,
                        provider.defaultEmbeddingModel()),
                resolveEmbeddingDimensions(entity, provider)
        );
    }

    public int getEffectiveEmbeddingDimensions() {
        return getEffectiveConfig().embeddingDimensions();
    }

    public AiConfigDto toAdminDto() {
        EffectiveAiConfig config = getEffectiveConfig();
        boolean configured = config.apiKey() != null && !config.apiKey().isBlank();
        AiRuntimeConfigEntity entity = findEntity();
        return AiConfigDto.builder()
                .provider(config.provider().id())
                .baseUrl(config.baseUrl())
                .embeddingDimensions(config.embeddingDimensions())
                .apiKeyMasked(ApiKeyMasker.mask(configured ? config.apiKey() : ""))
                .apiKeyConfigured(configured)
                .apiKeySource(config.apiKeySource())
                .routerModel(config.routerModel())
                .chatModel(config.chatModel())
                .embeddingModel(config.embeddingModel())
                .customChatModels(parseModelList(entity != null ? entity.getCustomChatModelsJson() : null, "custom chat models"))
                .customRouterModels(parseModelList(entity != null ? entity.getCustomRouterModelsJson() : null, "custom router models"))
                .build();
    }

    @Transactional
    public AiConfigDto update(AiConfigUpdateRequest request) {
        AiRuntimeConfigEntity entity = repository.findById(AiRuntimeConfigEntity.DEFAULT_ID)
                .orElseGet(() -> AiRuntimeConfigEntity.builder()
                        .id(AiRuntimeConfigEntity.DEFAULT_ID)
                        .provider(AiProvider.DASHSCOPE.id())
                        .build());

        if (request.getProvider() != null && !request.getProvider().isBlank()) {
            entity.setProvider(AiProvider.from(request.getProvider()).id());
        }
        if (request.getBaseUrl() != null) {
            String trimmed = request.getBaseUrl().trim();
            entity.setBaseUrl(trimmed.isEmpty() ? null : trimmed);
        }
        if (request.getEmbeddingDimensions() != null) {
            entity.setEmbeddingDimensions(request.getEmbeddingDimensions() > 0
                    ? request.getEmbeddingDimensions() : null);
        }
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
        if (request.getCustomChatModels() != null) {
            entity.setCustomChatModelsJson(serializeModelList(request.getCustomChatModels()));
        }
        if (request.getCustomRouterModels() != null) {
            entity.setCustomRouterModelsJson(serializeModelList(request.getCustomRouterModels()));
        }

        repository.save(entity);
        configVersion++;
        return toAdminDto();
    }

    private AiRuntimeConfigEntity findEntity() {
        return repository.findById(AiRuntimeConfigEntity.DEFAULT_ID).orElse(null);
    }

    private AiProvider resolveProvider(AiRuntimeConfigEntity entity) {
        if (entity != null && entity.getProvider() != null && !entity.getProvider().isBlank()) {
            return AiProvider.from(entity.getProvider());
        }
        return AiProvider.from(aiProperties.getProvider());
    }

    private String resolveEnvApiKey(AiProvider provider) {
        return provider == AiProvider.ZHIPUAI
                ? aiProperties.getZhipuai().getApiKey()
                : envDashScopeApiKey;
    }

    private String resolveBaseUrl(AiRuntimeConfigEntity entity, AiProvider provider) {
        if (provider != AiProvider.ZHIPUAI) {
            return null;
        }
        if (entity != null && entity.getBaseUrl() != null && !entity.getBaseUrl().isBlank()) {
            return entity.getBaseUrl().trim();
        }
        String envBaseUrl = aiProperties.getZhipuai().getBaseUrl();
        if (envBaseUrl != null && !envBaseUrl.isBlank()) {
            return envBaseUrl.trim();
        }
        return provider.defaultBaseUrl();
    }

    private int resolveEmbeddingDimensions(AiRuntimeConfigEntity entity, AiProvider provider) {
        if (entity != null && entity.getEmbeddingDimensions() != null && entity.getEmbeddingDimensions() > 0) {
            return entity.getEmbeddingDimensions();
        }
        return provider.defaultEmbeddingDimensions();
    }

    private String resolveModel(String dbValue, String propertyDefault, String providerDefault) {
        if (dbValue != null && !dbValue.isBlank()) {
            return dbValue;
        }
        if (propertyDefault != null && !propertyDefault.isBlank()) {
            return propertyDefault;
        }
        return providerDefault;
    }

    private List<String> parseModelList(String json, String label) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> models = objectMapper.readValue(json, new TypeReference<>() {});
            return normalizeModelNames(models);
        } catch (Exception e) {
            log.warn("Failed to parse {} json", label, e);
            return List.of();
        }
    }

    private String serializeModelList(List<String> models) {
        try {
            return objectMapper.writeValueAsString(normalizeModelNames(models));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize model list", e);
        }
    }

    private List<String> normalizeModelNames(List<String> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String model : models) {
            if (model != null && !model.isBlank()) {
                unique.add(model.trim());
            }
        }
        return new ArrayList<>(unique);
    }

    public record EffectiveAiConfig(
            AiProvider provider,
            String apiKey,
            String apiKeySource,
            String baseUrl,
            String routerModel,
            String chatModel,
            String embeddingModel,
            int embeddingDimensions
    ) {
    }
}
