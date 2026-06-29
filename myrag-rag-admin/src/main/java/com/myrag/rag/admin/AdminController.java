package com.myrag.rag.admin;

import com.myrag.common.dto.*;
import com.myrag.rag.core.service.AiRuntimeConfigService;
import com.myrag.rag.core.service.DocumentIngestionService;
import com.myrag.rag.core.service.KnowledgeBaseService;
import com.myrag.rag.core.service.RagQueryService;
import com.myrag.rag.core.service.SystemPromptService;
import com.myrag.rag.monitor.entity.RecallLogEntity;
import com.myrag.rag.monitor.entity.RecallQualityAlertEntity;
import com.myrag.rag.monitor.entity.ChatCacheLogEntity;
import com.myrag.rag.monitor.entity.ChatQueryLogEntity;
import com.myrag.rag.monitor.service.RecallLogService;
import com.myrag.rag.monitor.service.RecallQualityEvaluator;
import com.myrag.rag.monitor.service.ChatCacheMonitorService;
import com.myrag.rag.monitor.service.ChatQueryLogService;
import com.myrag.chat.cache.ChatCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentIngestionService documentIngestionService;
    private final SystemPromptService systemPromptService;
    private final AiRuntimeConfigService aiRuntimeConfigService;
    private final RagQueryService ragQueryService;
    private final RecallLogService recallLogService;
    private final RecallQualityEvaluator recallQualityEvaluator;
    private final ChatCacheMonitorService chatCacheMonitorService;
    private final ChatQueryLogService chatQueryLogService;
    private final ChatCacheService chatCacheService;

    // --- Knowledge Base ---

    @GetMapping("/knowledge-bases")
    public ApiResponse<List<KnowledgeBaseDto>> listKnowledgeBases() {
        return ApiResponse.ok(knowledgeBaseService.listAll());
    }

    @PostMapping("/knowledge-bases")
    public ApiResponse<KnowledgeBaseDto> createKnowledgeBase(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(knowledgeBaseService.create(
                body.get("name"), body.get("description"), body.get("chunkConfigJson")));
    }

    @PutMapping("/knowledge-bases/{id}")
    public ApiResponse<KnowledgeBaseDto> updateKnowledgeBase(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ApiResponse.ok(knowledgeBaseService.update(
                id, body.get("name"), body.get("description"), body.get("chunkConfigJson")));
    }

    @DeleteMapping("/knowledge-bases/{id}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return ApiResponse.ok(null);
    }

    // --- Documents ---

    @GetMapping("/knowledge-bases/{kbId}/documents")
    public ApiResponse<List<DocumentDto>> listDocuments(@PathVariable String kbId) {
        return ApiResponse.ok(documentIngestionService.listByKb(kbId));
    }

    @PostMapping("/knowledge-bases/{kbId}/documents")
    public ApiResponse<DocumentDto> uploadDocument(
            @PathVariable String kbId, @RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(documentIngestionService.upload(kbId, file));
    }

    @DeleteMapping("/documents/{docId}")
    public ApiResponse<Void> deleteDocument(@PathVariable String docId) {
        documentIngestionService.delete(docId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/documents/{docId}/reindex")
    public ApiResponse<DocumentDto> reindexDocument(@PathVariable String docId) throws IOException {
        return ApiResponse.ok(documentIngestionService.reindex(docId));
    }

    // --- System Prompt ---

    @GetMapping("/system-prompt")
    public ApiResponse<Map<String, String>> getSystemPrompt() {
        return ApiResponse.ok(Map.of("prompt", systemPromptService.getActivePrompt()));
    }

    @PutMapping("/system-prompt")
    public ApiResponse<Map<String, String>> updateSystemPrompt(@RequestBody Map<String, String> body) {
        String prompt = systemPromptService.updatePrompt(body.get("prompt"));
        return ApiResponse.ok(Map.of("prompt", prompt));
    }

    // --- AI Config ---

    @GetMapping("/ai-config")
    public ApiResponse<AiConfigDto> getAiConfig() {
        return ApiResponse.ok(aiRuntimeConfigService.toAdminDto());
    }

    @PutMapping("/ai-config")
    public ApiResponse<AiConfigDto> updateAiConfig(@RequestBody AiConfigUpdateRequest request) {
        return ApiResponse.ok(aiRuntimeConfigService.update(request));
    }

    // --- Recall Test ---

    @PostMapping("/recall-test")
    public ApiResponse<RagSearchResponse> recallTest(@RequestBody RagSearchRequest request) {
        RagSearchResponse response = ragQueryService.search(request);
        if (request.getKbIds() != null) {
            for (String kbId : request.getKbIds()) {
                recallQualityEvaluator.evaluateAsync(null, kbId, request.getQuery(),
                        response.getResults().stream()
                                .filter(r -> kbId.equals(r.getKbId()))
                                .toList());
            }
        }
        return ApiResponse.ok(response);
    }

    // --- Monitor ---

    @GetMapping("/monitor/recall-logs")
    public ApiResponse<Page<RecallLogEntity>> recallLogs(
            @RequestParam(required = false) String kbId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(recallLogService.listLogs(kbId, PageRequest.of(page, size)));
    }

    @GetMapping("/monitor/metrics")
    public ApiResponse<Map<String, Object>> metrics() {
        Map<String, Object> merged = new java.util.HashMap<>(recallLogService.getMetrics());
        merged.putAll(chatQueryLogService.getMetrics());
        return ApiResponse.ok(merged);
    }

    @GetMapping("/monitor/chat-logs")
    public ApiResponse<Page<ChatQueryLogEntity>> chatLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(chatQueryLogService.listLogs(PageRequest.of(page, size)));
    }

    @GetMapping("/monitor/cache-logs")
    public ApiResponse<Page<ChatCacheLogEntity>> cacheLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(chatCacheMonitorService.listLogs(PageRequest.of(page, size)));
    }

    @DeleteMapping("/monitor/cache")
    public ApiResponse<Map<String, Long>> clearCache() {
        long deleted = chatCacheService.clearAll();
        return ApiResponse.ok(Map.of("deletedKeys", deleted));
    }

    @GetMapping("/monitor/alerts")
    public ApiResponse<List<RecallQualityAlertEntity>> alerts() {
        return ApiResponse.ok(recallQualityEvaluator.recentAlerts());
    }
}
