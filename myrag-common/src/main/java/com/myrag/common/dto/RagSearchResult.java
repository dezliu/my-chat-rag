package com.myrag.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResult {
    private String content;
    private double score;
    private String docId;
    private String kbId;
    private Map<String, Object> metadata;
}
