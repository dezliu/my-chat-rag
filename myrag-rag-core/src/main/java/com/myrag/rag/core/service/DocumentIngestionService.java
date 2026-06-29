package com.myrag.rag.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myrag.common.ai.DynamicModelProvider;
import com.myrag.common.dto.DocumentDto;
import com.myrag.common.exception.MyragException;
import com.myrag.rag.core.entity.DocumentChunkEntity;
import com.myrag.rag.core.entity.DocumentEntity;
import com.myrag.rag.core.entity.KnowledgeBaseEntity;
import com.myrag.rag.core.qdrant.HybridSearchService;
import com.myrag.rag.core.repository.DocumentChunkRepository;
import com.myrag.rag.core.repository.DocumentRepository;
import com.myrag.rag.core.sparse.Bm25SparseEncoder;
import com.myrag.rag.core.util.TextSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final HybridSearchService hybridSearchService;
    private final DynamicModelProvider modelProvider;
    private final Bm25SparseEncoder bm25SparseEncoder;
    private final TextSplitter textSplitter;
    private final ObjectMapper objectMapper;
    private final Tika tika = new Tika();
    private final KbRevisionService kbRevisionService;

    public List<DocumentDto> listByKb(String kbId) {
        return documentRepository.findByKbId(kbId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DocumentDto upload(String kbId, MultipartFile file) throws IOException {
        KnowledgeBaseEntity kb = knowledgeBaseService.getEntity(kbId);
        String content = extractText(file);
        String hash = sha256(content);

        DocumentEntity doc = DocumentEntity.builder()
                .id(UUID.randomUUID().toString())
                .kbId(kbId)
                .filename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown")
                .contentHash(hash)
                .status("PROCESSING")
                .build();
        documentRepository.save(doc);

        try {
            int chunkCount = indexDocument(kb, doc, content);
            doc.setChunkCount(chunkCount);
            doc.setStatus("INDEXED");
        } catch (Exception e) {
            doc.setStatus("FAILED");
            log.error("Failed to index document {}", doc.getId(), e);
            throw new MyragException(500, "Document indexing failed: " + e.getMessage());
        }
        kbRevisionService.bumpRevision(kbId);
        return toDto(documentRepository.save(doc));
    }

    @Transactional
    public DocumentDto reindex(String docId) throws IOException {
        DocumentEntity doc = documentRepository.findById(docId)
                .orElseThrow(() -> new MyragException(404, "Document not found: " + docId));
        KnowledgeBaseEntity kb = knowledgeBaseService.getEntity(doc.getKbId());

        hybridSearchService.deleteByDocId(kb.getCollectionName(), docId);
        documentChunkRepository.deleteByDocId(docId);

        String content = documentChunkRepository.findByDocId(docId).stream()
                .map(DocumentChunkEntity::getContentPreview)
                .reduce("", (a, b) -> a + "\n" + b);
        if (content.isBlank()) {
            throw new MyragException(400, "No content available for reindex, please re-upload");
        }

        doc.setStatus("PROCESSING");
        int chunkCount = indexDocument(kb, doc, content);
        doc.setChunkCount(chunkCount);
        doc.setStatus("INDEXED");
        kbRevisionService.bumpRevision(doc.getKbId());
        return toDto(documentRepository.save(doc));
    }

    @Transactional
    public void delete(String docId) {
        DocumentEntity doc = documentRepository.findById(docId)
                .orElseThrow(() -> new MyragException(404, "Document not found: " + docId));
        KnowledgeBaseEntity kb = knowledgeBaseService.getEntity(doc.getKbId());
        hybridSearchService.deleteByDocId(kb.getCollectionName(), docId);
        documentChunkRepository.deleteByDocId(docId);
        documentRepository.delete(doc);
        kbRevisionService.bumpRevision(doc.getKbId());
    }

    private int indexDocument(KnowledgeBaseEntity kb, DocumentEntity doc, String content) {
        int chunkSize = 500;
        int chunkOverlap = 50;
        try {
            JsonNode config = objectMapper.readTree(kb.getChunkConfigJson());
            chunkSize = config.path("chunkSize").asInt(500);
            chunkOverlap = config.path("chunkOverlap").asInt(50);
        } catch (Exception ignored) {
            // use defaults
        }

        bm25SparseEncoder.indexDocument(doc.getId(), content);
        List<String> chunks = textSplitter.split(content, chunkSize, chunkOverlap);
        int index = 0;
        for (String chunk : chunks) {
            String pointId = UUID.randomUUID().toString();
            float[] dense = modelProvider.embeddingModel().embed(chunk);
            var sparse = bm25SparseEncoder.encode(chunk);

            hybridSearchService.upsertChunk(
                    kb.getCollectionName(), pointId, chunk,
                    doc.getId(), kb.getId(), index, dense, sparse);

            documentChunkRepository.save(DocumentChunkEntity.builder()
                    .docId(doc.getId())
                    .kbId(kb.getId())
                    .chunkIndex(index)
                    .contentPreview(chunk.length() > 200 ? chunk.substring(0, 200) : chunk)
                    .qdrantPointId(pointId)
                    .build());
            index++;
        }
        return chunks.size();
    }

    private String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.endsWith(".txt") || filename.endsWith(".md"))) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        try {
            return tika.parseToString(file.getInputStream());
        } catch (Exception e) {
            throw new IOException("Failed to parse document: " + filename, e);
        }
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private DocumentDto toDto(DocumentEntity entity) {
        return DocumentDto.builder()
                .id(entity.getId())
                .kbId(entity.getKbId())
                .filename(entity.getFilename())
                .contentHash(entity.getContentHash())
                .status(entity.getStatus())
                .chunkCount(entity.getChunkCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
