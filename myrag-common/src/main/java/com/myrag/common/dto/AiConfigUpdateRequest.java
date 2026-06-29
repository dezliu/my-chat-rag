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
public class AiConfigUpdateRequest {
    private String provider;
    private String baseUrl;
    private Integer embeddingDimensions;
    private String apiKey;
    private String routerModel;
    private String chatModel;
    private String embeddingModel;
    private List<String> customChatModels;
    private List<String> customRouterModels;
}
