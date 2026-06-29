package com.myrag.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiConfigUpdateRequest {
    private String apiKey;
    private String routerModel;
    private String chatModel;
    private String embeddingModel;
}
