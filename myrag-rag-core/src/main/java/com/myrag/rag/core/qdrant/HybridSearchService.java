package com.myrag.rag.core.qdrant;

import com.myrag.common.ai.DynamicModelProvider;
import com.myrag.common.config.HybridSearchProperties;
import com.myrag.common.dto.RagSearchResult;
import com.myrag.rag.core.sparse.Bm25SparseEncoder;
import com.myrag.rag.core.sparse.Bm25SparseEncoder.SparseVectorData;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.fusion;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.VectorFactory.vector;
import static io.qdrant.client.VectorsFactory.namedVectors;
import static io.qdrant.client.ValueFactory.value;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final QdrantClient qdrantClient;
    private final DynamicModelProvider modelProvider;
    private final Bm25SparseEncoder bm25SparseEncoder;
    private final HybridSearchProperties properties;
    private final QdrantCollectionManager collectionManager;

    public List<RagSearchResult> search(String collectionName, String query, int topK, double minScore) {
        collectionManager.ensureCollection(collectionName);
        long start = System.currentTimeMillis();
        try {
            float[] denseVector = modelProvider.embeddingModel().embed(query);
            SparseVectorData sparse = bm25SparseEncoder.encode(query);

            List<Float> denseList = new ArrayList<>(denseVector.length);
            for (float v : denseVector) {
                denseList.add(v);
            }

            List<Integer> sparseIndices = sparse.indices().stream()
                    .map(Long::intValue)
                    .collect(Collectors.toList());

            var densePrefetch = PrefetchQuery.newBuilder()
                    .setQuery(nearest(denseList))
                    .setUsing("dense")
                    .setLimit(properties.getDenseLimit())
                    .build();

            var sparsePrefetch = PrefetchQuery.newBuilder()
                    .setQuery(nearest(sparse.values(), sparseIndices))
                    .setUsing("sparse")
                    .setLimit(properties.getSparseLimit())
                    .build();

            var queryPoints = QueryPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addPrefetch(densePrefetch)
                    .addPrefetch(sparsePrefetch)
                    .setQuery(fusion(Fusion.RRF))
                    .setLimit(topK)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                    .build();

            List<ScoredPoint> points = qdrantClient.queryAsync(queryPoints).get();
            List<RagSearchResult> results = new ArrayList<>();
            for (ScoredPoint point : points) {
                if (point.getScore() < minScore) {
                    continue;
                }
                Map<String, JsonWithInt.Value> payload = point.getPayloadMap();
                String content = getPayloadString(payload, "content");
                String docId = getPayloadString(payload, "doc_id");
                String kbId = getPayloadString(payload, "kb_id");
                results.add(RagSearchResult.builder()
                        .content(content)
                        .score(point.getScore())
                        .docId(docId)
                        .kbId(kbId)
                        .metadata(Map.of("pointId", point.getId().getUuid()))
                        .build());
            }
            log.debug("Hybrid search on {} returned {} results in {}ms", collectionName, results.size(),
                    System.currentTimeMillis() - start);
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Search interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Hybrid search failed", e.getCause());
        }
    }

    public void upsertChunk(String collectionName, String pointId, String content,
                            String docId, String kbId, int chunkIndex, float[] denseVector,
                            SparseVectorData sparseVector) {
        collectionManager.ensureCollection(collectionName);
        try {
            List<Float> denseList = new ArrayList<>(denseVector.length);
            for (float v : denseVector) {
                denseList.add(v);
            }

            List<Integer> sparseIndices = sparseVector.indices().stream()
                    .map(Long::intValue)
                    .collect(Collectors.toList());

            Map<String, Points.Vector> named = Map.of(
                    "dense", vector(denseList),
                    "sparse", vector(sparseVector.values(), sparseIndices)
            );

            PointStruct point = PointStruct.newBuilder()
                    .setId(id(UUID.fromString(pointId)))
                    .setVectors(namedVectors(named))
                    .putAllPayload(Map.of(
                            "content", value(content),
                            "doc_id", value(docId),
                            "kb_id", value(kbId),
                            "chunk_index", value(chunkIndex)
                    ))
                    .build();

            qdrantClient.upsertAsync(collectionName, List.of(point)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upsert interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to upsert chunk", e.getCause());
        }
    }

    public void deleteByDocId(String collectionName, String docId) {
        try {
            var filter = Filter.newBuilder()
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey("doc_id")
                                    .setMatch(Match.newBuilder()
                                            .setKeyword(docId)
                                            .build())
                                    .build())
                            .build())
                    .build();
            qdrantClient.deleteAsync(collectionName, filter).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Delete interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to delete points for doc: " + docId, e.getCause());
        }
    }

    private String getPayloadString(Map<String, JsonWithInt.Value> payload, String key) {
        JsonWithInt.Value val = payload.get(key);
        if (val == null) {
            return "";
        }
        return val.getStringValue();
    }
}
