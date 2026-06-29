package com.myrag.rag.mcp;

import com.myrag.common.dto.KnowledgeBaseDto;
import com.myrag.common.dto.RagSearchRequest;
import com.myrag.common.dto.RagSearchResponse;
import com.myrag.common.dto.RagSearchResult;
import com.myrag.rag.core.service.KnowledgeBaseService;
import com.myrag.rag.core.service.RagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagMcpTools {

    private final RagQueryService ragQueryService;
    private final KnowledgeBaseService knowledgeBaseService;

    @Tool(description = "Search a knowledge base using hybrid RAG retrieval (dense + BM25)")
    public String searchKnowledgeBase(
            @ToolParam(description = "Knowledge base ID") String kbId,
            @ToolParam(description = "Search query") String query,
            @ToolParam(description = "Number of results to return", required = false) Integer topK) {
        RagSearchResponse response = ragQueryService.search(RagSearchRequest.builder()
                .kbIds(List.of(kbId))
                .query(query)
                .topK(topK != null ? topK : 5)
                .build());

        if (response.getResults().isEmpty()) {
            return "No results found for query: " + query;
        }

        return response.getResults().stream()
                .map(r -> "[score=" + String.format("%.3f", r.getScore()) + "] " + r.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    @Tool(description = "List all available knowledge bases")
    public String listKnowledgeBases() {
        List<KnowledgeBaseDto> kbs = knowledgeBaseService.listActive();
        if (kbs.isEmpty()) {
            return "No knowledge bases available.";
        }
        return kbs.stream()
                .map(kb -> "ID: " + kb.getId() + " | Name: " + kb.getName() + " | Description: " + kb.getDescription())
                .collect(Collectors.joining("\n"));
    }
}
