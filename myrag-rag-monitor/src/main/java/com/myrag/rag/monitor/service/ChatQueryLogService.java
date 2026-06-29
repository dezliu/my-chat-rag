package com.myrag.rag.monitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myrag.common.dto.RagRouteDecision;
import com.myrag.common.dto.RagSearchResult;
import com.myrag.rag.monitor.entity.ChatQueryLogEntity;
import com.myrag.rag.monitor.repository.ChatQueryLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatQueryLogService {

    private static final int REPLY_PREVIEW_MAX = 500;

    private final ChatQueryLogRepository chatQueryLogRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final RecallQualityEvaluator recallQualityEvaluator;

    public ChatQueryLogService(ChatQueryLogRepository chatQueryLogRepository,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry,
                               @Lazy RecallQualityEvaluator recallQualityEvaluator) {
        this.chatQueryLogRepository = chatQueryLogRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.recallQualityEvaluator = recallQualityEvaluator;
    }

    @Async
    public void logQueryAsync(String sessionId, String query, String reply, boolean cacheHit,
                              RagRouteDecision route, boolean usedRag, List<String> ragKbIds,
                              int recallCount, List<RagSearchResult> recallResults, long latencyMs) {
        try {
            String kbIdsJson = ragKbIds != null ? objectMapper.writeValueAsString(ragKbIds) : "[]";
            List<Double> scores = recallResults != null
                    ? recallResults.stream().map(RagSearchResult::getScore).collect(Collectors.toList())
                    : List.of();
            String scoresJson = objectMapper.writeValueAsString(scores);

            String preview = reply != null && reply.length() > REPLY_PREVIEW_MAX
                    ? reply.substring(0, REPLY_PREVIEW_MAX)
                    : reply;

            ChatQueryLogEntity entity = ChatQueryLogEntity.builder()
                    .sessionId(sessionId)
                    .query(query)
                    .replyPreview(preview)
                    .cacheHit(cacheHit)
                    .needRag(route != null ? route.isNeedRag() : null)
                    .usedRag(usedRag)
                    .ragKbIds(kbIdsJson)
                    .routeReason(route != null ? route.getReason() : null)
                    .routeConfidence(route != null ? route.getConfidence() : null)
                    .recallCount(recallCount >= 0 ? recallCount : null)
                    .topScoresJson(scoresJson)
                    .latencyMs(latencyMs)
                    .build();

            entity = chatQueryLogRepository.save(entity);

            meterRegistry.counter("chat.query.count").increment();
            if (cacheHit) {
                meterRegistry.counter("chat.cache.hit").increment();
            } else {
                meterRegistry.counter("chat.cache.miss").increment();
            }
            if (usedRag) {
                meterRegistry.counter("chat.rag.used").increment();
            }
            if (route != null && route.isNeedRag()) {
                meterRegistry.counter("chat.rag.needed").increment();
                if (recallCount > 0) {
                    meterRegistry.counter("chat.rag.recall.hit").increment();
                }
            }

            if (!cacheHit && usedRag && recallResults != null && !recallResults.isEmpty()) {
                recallQualityEvaluator.evaluateChatQueryAsync(entity.getId(), query, recallResults);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to log chat query", e);
        }
    }

    @Transactional
    public void updateQuality(String logId, double score, String reason) {
        chatQueryLogRepository.findById(logId).ifPresent(entity -> {
            entity.setQualityScore(score);
            entity.setQualityReason(reason);
            chatQueryLogRepository.save(entity);
        });
    }

    public Page<ChatQueryLogEntity> listLogs(Pageable pageable) {
        return chatQueryLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Map<String, Object> getMetrics() {
        double totalQueries = meterRegistry.find("chat.query.count").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double cacheHits = meterRegistry.find("chat.cache.hit").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double cacheMisses = meterRegistry.find("chat.cache.miss").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double ragUsed = meterRegistry.find("chat.rag.used").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double ragNeeded = meterRegistry.find("chat.rag.needed").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double recallHit = meterRegistry.find("chat.rag.recall.hit").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double cacheTotal = cacheHits + cacheMisses;

        double avgQuality = chatQueryLogRepository.findAll().stream()
                .filter(e -> e.getQualityScore() != null)
                .mapToDouble(ChatQueryLogEntity::getQualityScore)
                .average()
                .orElse(0.0);

        return Map.of(
                "totalChatQueries", (long) totalQueries,
                "totalCacheHits", (long) cacheHits,
                "totalCacheMisses", (long) cacheMisses,
                "cacheHitRate", cacheTotal > 0 ? cacheHits / cacheTotal : 0.0,
                "chatRagRate", totalQueries > 0 ? ragUsed / totalQueries : 0.0,
                "chatRecallRate", ragNeeded > 0 ? recallHit / ragNeeded : 0.0,
                "avgQualityScore", avgQuality
        );
    }
}
