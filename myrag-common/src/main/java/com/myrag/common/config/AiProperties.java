package com.myrag.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "myrag.ai")
public class AiProperties {

    private String provider = "dashscope";

    private ZhipuAi zhipuai = new ZhipuAi();

    @Data
    public static class ZhipuAi {
        private String apiKey = "";
        private String baseUrl = "https://open.bigmodel.cn/api/paas";
    }
}
