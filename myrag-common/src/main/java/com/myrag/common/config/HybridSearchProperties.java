package com.myrag.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "myrag.rag.hybrid")
public class HybridSearchProperties {
    private int denseLimit = 20;
    private int sparseLimit = 20;
    private int finalTopK = 5;
    private String fusion = "rrf";
    private int embeddingDimensions = 1024;
}
