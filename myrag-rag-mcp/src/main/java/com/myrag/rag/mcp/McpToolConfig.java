package com.myrag.rag.mcp;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpToolConfig {

    @Bean
    public List<ToolCallback> ragMcpToolCallbacks(RagMcpTools ragMcpTools) {
        return List.of(ToolCallbacks.from(ragMcpTools));
    }
}
