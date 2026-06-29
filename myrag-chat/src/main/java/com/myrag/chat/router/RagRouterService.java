package com.myrag.chat.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myrag.common.config.ChatProperties;
import com.myrag.common.dto.KnowledgeBaseDto;
import com.myrag.common.dto.RagRouteDecision;
import com.myrag.rag.core.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagRouterService {

    private final ChatClient.Builder chatClientBuilder;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatProperties chatProperties;
    private final ObjectMapper objectMapper;

    public RagRouteDecision route(String userMessage) {
        List<KnowledgeBaseDto> kbs = knowledgeBaseService.listActive();
        if (kbs.isEmpty()) {
            return RagRouteDecision.builder()
                    .needRag(false)
                    .kbIds(Collections.emptyList())
                    .reason("No knowledge bases available")
                    .confidence(1.0)
                    .build();
        }

        String kbList = kbs.stream()
                .map(kb -> "- id: " + kb.getId() + ", name: " + kb.getName() + ", description: " + kb.getDescription())
                .collect(Collectors.joining("\n"));

        String prompt = """
                你是一个 RAG 路由助手。根据用户问题，判断是否需要检索知识库，以及检索哪些知识库。
                
                可用知识库：
                %s
                
                用户问题：%s
                
                请严格以 JSON 格式返回（不要包含 markdown 代码块）：
                {"needRag": true/false, "kbIds": ["id1"], "reason": "原因", "confidence": 0.0-1.0}
                
                规则：
                1. 如果问题是闲聊、问候、通用常识，needRag 为 false
                2. 如果问题需要专业知识库内容，needRag 为 true 并选择最相关的 kbIds
                3. kbIds 只能从上面的 id 列表中选择
                """.formatted(kbList, userMessage);

        try {
            String response = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            String json = extractJson(response);
            return objectMapper.readValue(json, RagRouteDecision.class);
        } catch (Exception e) {
            log.warn("RAG routing failed, falling back to no-RAG", e);
            return RagRouteDecision.builder()
                    .needRag(false)
                    .kbIds(Collections.emptyList())
                    .reason("Routing failed: " + e.getMessage())
                    .confidence(0.0)
                    .build();
        }
    }

    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
