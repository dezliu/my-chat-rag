package com.myrag.rag.core.service;

import com.myrag.common.dto.RagSearchRequest;
import com.myrag.common.dto.RagSearchResponse;
import com.myrag.common.dto.RagSearchResult;
import com.myrag.rag.core.entity.KnowledgeBaseEntity;
import com.myrag.rag.core.qdrant.HybridSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RagQueryService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final HybridSearchService hybridSearchService;

    public RagSearchResponse search(RagSearchRequest request) {
        long start = System.currentTimeMillis();
        List<RagSearchResult> allResults = new ArrayList<>();

        for (String kbId : request.getKbIds()) {
            KnowledgeBaseEntity kb = knowledgeBaseService.getEntity(kbId);
            if (!"ACTIVE".equals(kb.getStatus())) {
                continue;
            }
            List<RagSearchResult> results = hybridSearchService.search(
                    kb.getCollectionName(),
                    request.getQuery(),
                    request.getTopK(),
                    request.getMinScore());
            allResults.addAll(results);
        }

        allResults.sort(Comparator.comparingDouble(RagSearchResult::getScore).reversed());
        if (allResults.size() > request.getTopK()) {
            allResults = allResults.subList(0, request.getTopK());
        }

        return RagSearchResponse.builder()
                .query(request.getQuery())
                .results(allResults)
                .latencyMs(System.currentTimeMillis() - start)
                .build();
    }
}
