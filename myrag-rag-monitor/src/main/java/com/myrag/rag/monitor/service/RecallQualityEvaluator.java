package com.myrag.rag.monitor.service;

import com.myrag.common.ai.DynamicModelProvider;
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

    private final DynamicModelProvider modelProvider;
    private final RecallQualityAlertRepository alertRepository;
    private final ChatQueryLogService chatQueryLogService;

    @Async
    public void evaluateAsync(String recallLogId, String kbId, String query, List<RagSearchResult> results) {
        if (results.isEmpty()) {
            saveAlert(recallLogId, kbId, query, 0.0, "No results returned");
            return;
        }
        QualityResult result = evaluate(query, results);
        if (result.score() < 0.5) {
            saveAlert(recallLogId, kbId, query, result.score(), result.reason());
        }
    }

    @Async
    public void evaluateChatQueryAsync(String chatQueryLogId, String query, List<RagSearchResult> results) {
        if (results.isEmpty()) {
            chatQueryLogService.updateQuality(chatQueryLogId, 0.0, "No results returned");
            return;
        }
        try {
            QualityResult result = evaluate(query, results);
            chatQueryLogService.updateQuality(chatQueryLogId, result.score(), result.reason());
            String kbId = results.get(0).getKbId();
            if (result.score() < 0.5) {
                saveAlert(null, kbId, query, result.score(), result.reason());
            }
        } catch (Exception e) {
            log.warn("Chat query quality evaluation failed", e);
        }
    }

    private QualityResult evaluate(String query, List<RagSearchResult> results) {
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

        String response = ChatClient.builder(modelProvider.chatChatModel())
                .build()
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
        return new QualityResult(score, reason);
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

    private record QualityResult(double score, String reason) {
    }
}
