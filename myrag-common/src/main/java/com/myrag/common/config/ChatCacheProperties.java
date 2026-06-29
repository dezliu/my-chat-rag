package com.myrag.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "myrag.chat.cache")
public class ChatCacheProperties {
    private boolean enabled = true;
    private int ttlHours = 24;
    private int minQuestionLength = 2;
}
