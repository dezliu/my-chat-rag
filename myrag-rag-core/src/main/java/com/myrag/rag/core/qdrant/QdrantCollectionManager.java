package com.myrag.rag.core.qdrant;

import com.myrag.rag.core.service.AiRuntimeConfigService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantCollectionManager {

    private final QdrantClient qdrantClient;
    private final AiRuntimeConfigService aiRuntimeConfigService;

    public void ensureCollection(String collectionName) {
        try {
            boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
            if (exists) {
                return;
            }

            int dimensions = aiRuntimeConfigService.getEffectiveEmbeddingDimensions();
            var denseParams = VectorParams.newBuilder()
                    .setSize(dimensions)
                    .setDistance(Distance.Cosine)
                    .build();

            var vectorsConfig = Collections.VectorsConfig.newBuilder()
                    .setParamsMap(Collections.VectorParamsMap.newBuilder()
                            .putMap("dense", denseParams)
                            .build())
                    .build();

            var sparseConfig = Collections.SparseVectorConfig.newBuilder()
                    .putMap("sparse", Collections.SparseVectorParams.newBuilder()
                            .setModifier(Collections.Modifier.Idf)
                            .build())
                    .build();

            qdrantClient.createCollectionAsync(
                    Collections.CreateCollection.newBuilder()
                            .setCollectionName(collectionName)
                            .setVectorsConfig(vectorsConfig)
                            .setSparseVectorsConfig(sparseConfig)
                            .build()
            ).get();
            log.info("Created Qdrant collection: {}", collectionName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating collection: " + collectionName, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to create collection: " + collectionName, e.getCause());
        }
    }

    public void deleteCollection(String collectionName) {
        try {
            if (qdrantClient.collectionExistsAsync(collectionName).get()) {
                qdrantClient.deleteCollectionAsync(collectionName).get();
                log.info("Deleted Qdrant collection: {}", collectionName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while deleting collection: " + collectionName, e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to delete collection: " + collectionName, e.getCause());
        }
    }
}
