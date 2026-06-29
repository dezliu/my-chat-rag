package com.myrag.rag.query;

import com.myrag.common.dto.ApiResponse;
import com.myrag.common.dto.KnowledgeBaseDto;
import com.myrag.common.dto.RagSearchRequest;
import com.myrag.common.dto.RagSearchResponse;
import com.myrag.rag.core.service.KnowledgeBaseService;
import com.myrag.rag.core.service.RagQueryService;
import com.myrag.rag.monitor.service.RecallLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagQueryController {

    private final RagQueryService ragQueryService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final RecallLogService recallLogService;

    @PostMapping("/search")
    public ApiResponse<RagSearchResponse> search(@Valid @RequestBody RagSearchRequest request) {
        RagSearchResponse response = ragQueryService.search(request);
        if (request.getKbIds() != null) {
            for (String kbId : request.getKbIds()) {
                recallLogService.logRecall(kbId, request.getQuery(), response.getResults().size(),
                        response.getResults(), response.getLatencyMs());
            }
        }
        return ApiResponse.ok(response);
    }

    @GetMapping("/knowledge-bases")
    public ApiResponse<List<KnowledgeBaseDto>> listKnowledgeBases() {
        return ApiResponse.ok(knowledgeBaseService.listActive());
    }
}
