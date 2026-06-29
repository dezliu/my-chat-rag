package com.myrag.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchRequest {
    private List<String> kbIds;
    private String query;
    @Builder.Default
    private int topK = 5;
    @Builder.Default
    private double minScore = 0.0;
}
