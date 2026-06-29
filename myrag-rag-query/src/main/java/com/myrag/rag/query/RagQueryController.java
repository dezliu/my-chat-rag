package com.myrag.rag.query;

import com.myrag.common.dto.ApiResponse;
import com.myrag.common.dto.KnowledgeBaseDto;
import com.myrag.common.dto.RagSearchRequest;
import com.myrag.common.dto.RagSearchResponse;
import com.myrag.rag.core.service.KnowledgeBaseService;
import com.myrag.rag.core.service.RagQueryService;
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

    @PostMapping("/search")
    public ApiResponse<RagSearchResponse> search(@Valid @RequestBody RagSearchRequest request) {
        return ApiResponse.ok(ragQueryService.search(request));
    }

    @GetMapping("/knowledge-bases")
    public ApiResponse<List<KnowledgeBaseDto>> listKnowledgeBases() {
        return ApiResponse.ok(knowledgeBaseService.listActive());
    }
}
