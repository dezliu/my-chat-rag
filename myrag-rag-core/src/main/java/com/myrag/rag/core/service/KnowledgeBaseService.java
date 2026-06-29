package com.myrag.rag.core.service;

import com.myrag.common.dto.KnowledgeBaseDto;
import com.myrag.common.exception.MyragException;
import com.myrag.rag.core.entity.KnowledgeBaseEntity;
import com.myrag.rag.core.qdrant.QdrantCollectionManager;
import com.myrag.rag.core.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final QdrantCollectionManager collectionManager;

    public List<KnowledgeBaseDto> listActive() {
        return knowledgeBaseRepository.findByStatus("ACTIVE").stream()
                .map(this::toDto)
                .toList();
    }

    public List<KnowledgeBaseDto> listAll() {
        return knowledgeBaseRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public KnowledgeBaseDto getById(String id) {
        return knowledgeBaseRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new MyragException(404, "Knowledge base not found: " + id));
    }

    @Transactional
    public KnowledgeBaseDto create(String name, String description, String chunkConfigJson) {
        String id = UUID.randomUUID().toString();
        String collectionName = "kb_" + id.replace("-", "").substring(0, 12);
        KnowledgeBaseEntity entity = KnowledgeBaseEntity.builder()
                .id(id)
                .name(name)
                .description(description)
                .collectionName(collectionName)
                .status("ACTIVE")
                .chunkConfigJson(chunkConfigJson != null ? chunkConfigJson : "{\"chunkSize\":500,\"chunkOverlap\":50}")
                .build();
        collectionManager.ensureCollection(collectionName);
        return toDto(knowledgeBaseRepository.save(entity));
    }

    @Transactional
    public KnowledgeBaseDto update(String id, String name, String description, String chunkConfigJson) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new MyragException(404, "Knowledge base not found: " + id));
        if (name != null) {
            entity.setName(name);
        }
        if (description != null) {
            entity.setDescription(description);
        }
        if (chunkConfigJson != null) {
            entity.setChunkConfigJson(chunkConfigJson);
        }
        return toDto(knowledgeBaseRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new MyragException(404, "Knowledge base not found: " + id));
        entity.setStatus("DELETED");
        knowledgeBaseRepository.save(entity);
    }

    public KnowledgeBaseEntity getEntity(String id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new MyragException(404, "Knowledge base not found: " + id));
    }

    private KnowledgeBaseDto toDto(KnowledgeBaseEntity entity) {
        return KnowledgeBaseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .collectionName(entity.getCollectionName())
                .status(entity.getStatus())
                .chunkConfigJson(entity.getChunkConfigJson())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
