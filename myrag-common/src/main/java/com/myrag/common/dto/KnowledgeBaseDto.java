package com.myrag.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseDto {
    private String id;
    private String name;
    private String description;
    private String collectionName;
    private String status;
    private String chunkConfigJson;
    private Instant createdAt;
}
