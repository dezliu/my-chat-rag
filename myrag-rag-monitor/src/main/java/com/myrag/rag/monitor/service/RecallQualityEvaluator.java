package com.myrag.rag.monitor.service;

import com.myrag.common.dto.RagSearchResult;
import com.myrag.rag.monitor.entity.RecallQualityAlertEntity;
import com.myrag.rag.monitor.repository.RecallQualityAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecallQualityEvaluator {

    private final ChatClient.Builder chatClientBuilder;
    private final RecallQualityAlertRepository alertRepository;

    @Async
    public void evaluateAsync(String recallLogId, String kbId, String query, List<RagSearchResult> results) {
        if (results.isEmpty()) {
            saveAlert(recallLogId, kbId, query, 0.0, "No results returned");
            return;
        }
        try {
            String context = results.stream()
                    .map(RagSearchResult::getContent)
                    .limit(3)
                    .collect(Collectors.joining("\n---\n"));

            String prompt = """
                    评估以下检索结果与用户查询的相关性，返回 0 到 1 之间的分数（仅返回数字）和一句话理由，格式：分数|理由
                    用户查询：%s
                    检索结果：
                    %s
                    """.formatted(query, context);

            String response = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            double score = 0.5;
            String reason = response;
            if (response != null && response.contains("|")) {
                String[] parts = response.split("\\|", 2);
                try {
                    score = Double.parseDouble(parts[0].trim());
                } catch (NumberFormatException ignored) {
                    // keep default
                }
                reason = parts.length > 1 ? parts[1].trim() : response;
            }

            if (score < 0.5) {
                saveAlert(recallLogId, kbId, query, score, reason);
            }
        } catch (Exception e) {
            log.warn("Recall quality evaluation failed", e);
        }
    }

    private void saveAlert(String recallLogId, String kbId, String query, double score, String reason) {
        alertRepository.save(RecallQualityAlertEntity.builder()
                .recallLogId(recallLogId)
                .kbId(kbId)
                .query(query)
                .qualityScore(score)
                .reason(reason)
                .build());
    }

    public List<RecallQualityAlertEntity> recentAlerts() {
        return alertRepository.findTop20ByOrderByCreatedAtDesc();
    }
}
