package com.myrag.rag.monitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myrag.common.dto.RagSearchResult;
import com.myrag.rag.monitor.entity.RecallLogEntity;
import com.myrag.rag.monitor.repository.RecallLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecallLogService {

    private final RecallLogRepository recallLogRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Async
    public void logRecall(String kbId, String query, int resultCount,
                          List<RagSearchResult> results, long latencyMs) {
        try {
            List<Double> scores = results.stream().map(RagSearchResult::getScore).collect(Collectors.toList());
            String scoresJson = objectMapper.writeValueAsString(scores);

            recallLogRepository.save(RecallLogEntity.builder()
                    .kbId(kbId)
                    .query(query)
                    .resultCount(resultCount)
                    .topScoresJson(scoresJson)
                    .latencyMs(latencyMs)
                    .build());

            meterRegistry.counter("rag.recall.count", "kb_id", kbId).increment();
            meterRegistry.timer("rag.recall.latency", "kb_id", kbId).record(java.time.Duration.ofMillis(latencyMs));
            if (resultCount == 0) {
                meterRegistry.counter("rag.recall.empty", "kb_id", kbId).increment();
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to log recall", e);
        }
    }

    public Page<RecallLogEntity> listLogs(String kbId, Pageable pageable) {
        if (kbId != null && !kbId.isBlank()) {
            return recallLogRepository.findByKbId(kbId, pageable);
        }
        return recallLogRepository.findAll(pageable);
    }

    public Map<String, Object> getMetrics() {
        double emptyRate = meterRegistry.find("rag.recall.empty").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double totalCount = meterRegistry.find("rag.recall.count").counters().stream()
                .mapToDouble(c -> c.count()).sum();
        double avgLatency = meterRegistry.find("rag.recall.latency").timers().stream()
                .mapToDouble(t -> t.mean(TimeUnit.MILLISECONDS))
                .average()
                .orElse(0.0);

        return Map.of(
                "totalRecalls", (long) totalCount,
                "emptyRate", totalCount > 0 ? emptyRate / totalCount : 0.0,
                "avgLatencyMs", avgLatency
        );
    }
}
