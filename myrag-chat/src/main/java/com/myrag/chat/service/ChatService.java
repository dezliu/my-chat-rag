package com.myrag.chat.service;

import com.myrag.common.config.ChatProperties;
import com.myrag.common.dto.*;
import com.myrag.chat.guard.PromptGuardService;
import com.myrag.chat.router.RagRouterService;
import com.myrag.rag.core.service.RagQueryService;
import com.myrag.rag.core.service.SystemPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final PromptGuardService promptGuardService;
    private final RagRouterService ragRouterService;
    private final RagQueryService ragQueryService;
    private final SystemPromptService systemPromptService;
    private final ChatProperties chatProperties;

    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();

    public ChatResponse chat(ChatRequest request) {
        String sanitized = promptGuardService.sanitize(request.getMessage(), chatProperties.getMaxUserTokens());
        RagRouteDecision route = ragRouterService.route(sanitized);

        String ragContext = "";
        boolean usedRag = false;
        if (route.isNeedRag() && route.getKbIds() != null && !route.getKbIds().isEmpty()) {
            RagSearchResponse searchResponse = ragQueryService.search(RagSearchRequest.builder()
                    .kbIds(route.getKbIds())
                    .query(sanitized)
                    .topK(5)
                    .build());
            if (!searchResponse.getResults().isEmpty()) {
                usedRag = true;
                ragContext = buildRagContext(searchResponse.getResults());
            }
        }

        String systemPrompt = systemPromptService.getActivePrompt();
        ChatClient client = buildChatClient(request.getSessionId(), systemPrompt);

        var promptSpec = client.prompt().user(sanitized);
        if (!ragContext.isBlank()) {
            promptSpec = client.prompt()
                    .system("以下是从知识库检索到的相关上下文，请基于这些内容回答用户问题：\n\n" + ragContext)
                    .user(sanitized);
        }

        String reply = promptSpec.call().content();

        return ChatResponse.builder()
                .sessionId(request.getSessionId())
                .reply(reply)
                .usedRag(usedRag)
                .ragKbIds(route.getKbIds())
                .routeDecision(route)
                .build();
    }

    public Flux<String> chatStream(ChatRequest request) {
        String sanitized = promptGuardService.sanitize(request.getMessage(), chatProperties.getMaxUserTokens());
        RagRouteDecision route = ragRouterService.route(sanitized);

        String ragContext = "";
        if (route.isNeedRag() && route.getKbIds() != null && !route.getKbIds().isEmpty()) {
            RagSearchResponse searchResponse = ragQueryService.search(RagSearchRequest.builder()
                    .kbIds(route.getKbIds())
                    .query(sanitized)
                    .topK(5)
                    .build());
            if (!searchResponse.getResults().isEmpty()) {
                ragContext = buildRagContext(searchResponse.getResults());
            }
        }

        String systemPrompt = systemPromptService.getActivePrompt();
        ChatClient client = buildChatClient(request.getSessionId(), systemPrompt);

        if (!ragContext.isBlank()) {
            return client.prompt()
                    .system("以下是从知识库检索到的相关上下文，请基于这些内容回答用户问题：\n\n" + ragContext)
                    .user(sanitized)
                    .stream()
                    .content();
        }
        return client.prompt().user(sanitized).stream().content();
    }

    private ChatClient buildChatClient(String sessionId, String systemPrompt) {
        ChatMemory memory = sessionMemories.computeIfAbsent(sessionId, id ->
                MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(20)
                        .build());

        return chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }

    private String buildRagContext(List<RagSearchResult> results) {
        String context = results.stream()
                .map(RagSearchResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));
        int maxLen = chatProperties.getMaxRagContextTokens() * 4;
        if (context.length() > maxLen) {
            context = context.substring(0, maxLen);
        }
        return context;
    }
}
