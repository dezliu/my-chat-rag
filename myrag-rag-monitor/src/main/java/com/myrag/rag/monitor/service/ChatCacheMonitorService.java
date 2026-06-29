package com.myrag.rag.monitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myrag.rag.monitor.entity.ChatCacheLogEntity;
import com.myrag.rag.monitor.repository.ChatCacheLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCacheMonitorService {

    private final ChatCacheLogRepository chatCacheLogRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Async
    public void logAccess(String query, boolean hit, boolean usedRag, List<String> kbIds, long latencyMs) {
        try {
            String kbIdsJson = kbIds != null ? objectMapper.writeValueAsString(kbIds) : "[]";
            chatCacheLogRepository.save(ChatCacheLogEntity.builder()
                    .query(query)
                    .hit(hit)
                    .usedRag(usedRag)
                    .kbIds(kbIdsJson)
                    .latencyMs(latencyMs)
                    .build());

            if (hit) {
                meterRegistry.counter("chat.cache.hit").increment();
            } else {
                meterRegistry.counter("chat.cache.miss").increment();
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to log chat cache access", e);
        }
    }

    public Page<ChatCacheLogEntity> listLogs(Pageable pageable) {
        return chatCacheLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Map<String, Object> getMetrics() {
        double hits = meterRegistry.find("chat.cache.hit").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double misses = meterRegistry.find("chat.cache.miss").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double total = hits + misses;
        return Map.of(
                "totalCacheHits", (long) hits,
                "totalCacheMisses", (long) misses,
                "cacheHitRate", total > 0 ? hits / total : 0.0
        );
    }
}
