package com.myrag.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigDto {
    private String apiKeyMasked;
    private boolean apiKeyConfigured;
    private String apiKeySource;
    private String routerModel;
    private String chatModel;
    private String embeddingModel;
    private List<String> customChatModels;
    private List<String> customRouterModels;
}
