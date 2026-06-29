package com.myrag.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "myrag.chat")
public class ChatProperties {
    private String routerModel = "qwen-turbo";
    private String chatModel = "qwen-plus";
    private int maxUserTokens = 4096;
    private int maxRagContextTokens = 8192;
    private String defaultSystemPrompt = "你是一个专业的智能助手。请根据提供的知识库上下文回答用户问题。如果上下文中没有相关信息，请明确告知用户。";
}
