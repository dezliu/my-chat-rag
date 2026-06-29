package com.myrag.chat.service;

import com.myrag.common.ai.DynamicModelProvider;
import com.myrag.common.config.ChatProperties;
import com.myrag.common.dto.*;
import com.myrag.chat.cache.ChatCacheService;
import com.myrag.chat.guard.PromptGuardService;
import com.myrag.chat.router.RagRouterService;
import com.myrag.rag.core.service.RagQueryService;
import com.myrag.rag.core.service.SystemPromptService;
import com.myrag.rag.monitor.service.ChatQueryLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final DynamicModelProvider modelProvider;
    private final PromptGuardService promptGuardService;
    private final RagRouterService ragRouterService;
    private final RagQueryService ragQueryService;
    private final SystemPromptService systemPromptService;
    private final ChatProperties chatProperties;
    private final ChatCacheService chatCacheService;
    private final ChatQueryLogService chatQueryLogService;

    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();

    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        String sanitized = promptGuardService.sanitize(request.getMessage(), chatProperties.getMaxUserTokens());
        RagRouteDecision route = ragRouterService.route(sanitized);

        if (chatCacheService.isEnabledFor(sanitized)) {
            String cacheKey = chatCacheService.buildRedisKey(sanitized, route);
            var cached = chatCacheService.get(cacheKey);
            if (cached.isPresent()) {
                CachedChatAnswer answer = cached.get();
                logQuery(request, sanitized, route, answer.getReply(), true, answer.isUsedRag(),
                        answer.getRagKbIds(), -1, List.of(), start);
                return ChatResponse.builder()
                        .sessionId(request.getSessionId())
                        .reply(answer.getReply())
                        .usedRag(answer.isUsedRag())
                        .ragKbIds(answer.getRagKbIds())
                        .routeDecision(route)
                        .cacheHit(true)
                        .build();
            }
        }

        ChatPipelineResult result = executePipeline(sanitized, request.getSessionId(), route);
        maybePutCache(sanitized, route, result);
        logQuery(request, sanitized, route, result.reply(), false, result.usedRag(),
                result.ragKbIds(), result.recallCount(), result.recallResults(), start);

        return ChatResponse.builder()
                .sessionId(request.getSessionId())
                .reply(result.reply())
                .usedRag(result.usedRag())
                .ragKbIds(result.ragKbIds())
                .routeDecision(route)
                .cacheHit(false)
                .build();
    }

    public Flux<String> chatStream(ChatRequest request) {
        long start = System.currentTimeMillis();
        String sanitized = promptGuardService.sanitize(request.getMessage(), chatProperties.getMaxUserTokens());
        RagRouteDecision route = ragRouterService.route(sanitized);

        if (chatCacheService.isEnabledFor(sanitized)) {
            String cacheKey = chatCacheService.buildRedisKey(sanitized, route);
            var cached = chatCacheService.get(cacheKey);
            if (cached.isPresent()) {
                CachedChatAnswer answer = cached.get();
                logQuery(request, sanitized, route, answer.getReply(), true, answer.isUsedRag(),
                        answer.getRagKbIds(), -1, List.of(), start);
                return streamFromCached(answer.getReply());
            }
        }

        RagSearchResponse searchResponse = searchIfNeeded(sanitized, route);
        boolean usedRag = searchResponse != null && !searchResponse.getResults().isEmpty();
        List<String> ragKbIds = route.getKbIds() != null ? route.getKbIds() : List.of();
        String ragContext = usedRag ? buildRagContext(searchResponse.getResults()) : "";

        String systemPrompt = systemPromptService.getActivePrompt();
        ChatClient client = buildChatClient(request.getSessionId(), systemPrompt);

        Flux<String> flux;
        if (!ragContext.isBlank()) {
            flux = client.prompt()
                    .system("以下是从知识库检索到的相关上下文，请基于这些内容回答用户问题：\n\n" + ragContext)
                    .user(sanitized)
                    .stream()
                    .content();
        } else {
            flux = client.prompt().user(sanitized).stream().content();
        }

        List<RagSearchResult> recallResults = searchResponse != null
                ? searchResponse.getResults() : List.of();
        int recallCount = recallResults.size();
        StringBuilder replyBuilder = new StringBuilder();

        return flux
                .doOnNext(replyBuilder::append)
                .doOnComplete(() -> {
                    String reply = replyBuilder.toString();
                    ChatPipelineResult result = new ChatPipelineResult(
                            reply, usedRag, ragKbIds, recallCount, recallResults);
                    maybePutCache(sanitized, route, result);
                    logQuery(request, sanitized, route, reply, false, usedRag,
                            ragKbIds, recallCount, recallResults, start);
                });
    }

    private void logQuery(ChatRequest request, String query, RagRouteDecision route, String reply,
                          boolean cacheHit, boolean usedRag, List<String> ragKbIds,
                          int recallCount, List<RagSearchResult> recallResults, long start) {
        chatQueryLogService.logQueryAsync(
                request.getSessionId(), query, reply, cacheHit, route, usedRag, ragKbIds,
                recallCount, recallResults, System.currentTimeMillis() - start);
    }

    private ChatPipelineResult executePipeline(String sanitized, String sessionId, RagRouteDecision route) {
        RagSearchResponse searchResponse = searchIfNeeded(sanitized, route);
        boolean usedRag = searchResponse != null && !searchResponse.getResults().isEmpty();
        List<String> ragKbIds = route.getKbIds() != null ? route.getKbIds() : List.of();
        String ragContext = usedRag ? buildRagContext(searchResponse.getResults()) : "";

        String systemPrompt = systemPromptService.getActivePrompt();
        ChatClient client = buildChatClient(sessionId, systemPrompt);

        String reply;
        if (!ragContext.isBlank()) {
            reply = client.prompt()
                    .system("以下是从知识库检索到的相关上下文，请基于这些内容回答用户问题：\n\n" + ragContext)
                    .user(sanitized)
                    .call()
                    .content();
        } else {
            reply = client.prompt().user(sanitized).call().content();
        }

        List<RagSearchResult> recallResults = searchResponse != null
                ? searchResponse.getResults() : Collections.emptyList();
        return new ChatPipelineResult(reply, usedRag, ragKbIds, recallResults.size(), recallResults);
    }

    private RagSearchResponse searchIfNeeded(String sanitized, RagRouteDecision route) {
        List<String> ragKbIds = route.getKbIds() != null ? route.getKbIds() : List.of();
        if (!route.isNeedRag() || ragKbIds.isEmpty()) {
            return null;
        }
        return ragQueryService.search(RagSearchRequest.builder()
                .kbIds(ragKbIds)
                .query(sanitized)
                .topK(5)
                .build());
    }

    private void maybePutCache(String sanitized, RagRouteDecision route, ChatPipelineResult result) {
        if (!chatCacheService.isEnabledFor(sanitized) || result.reply() == null || result.reply().isBlank()) {
            return;
        }
        String cacheKey = chatCacheService.buildRedisKey(sanitized, route);
        chatCacheService.put(cacheKey, CachedChatAnswer.builder()
                .reply(result.reply())
                .usedRag(result.usedRag())
                .ragKbIds(result.ragKbIds())
                .build());
    }

    private Flux<String> streamFromCached(String text) {
        if (text == null || text.isEmpty()) {
            return Flux.empty();
        }
        List<String> chunks = new ArrayList<>();
        int chunkSize = 8;
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(i + chunkSize, text.length())));
        }
        return Flux.fromIterable(chunks);
    }

    private ChatClient buildChatClient(String sessionId, String systemPrompt) {
        ChatMemory memory = sessionMemories.computeIfAbsent(sessionId, id ->
                MessageWindowChatMemory.builder()
                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                        .maxMessages(20)
                        .build());

        return ChatClient.builder(modelProvider.chatChatModel())
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

    private record ChatPipelineResult(String reply, boolean usedRag, List<String> ragKbIds,
                                      int recallCount, List<RagSearchResult> recallResults) {
    }
}
