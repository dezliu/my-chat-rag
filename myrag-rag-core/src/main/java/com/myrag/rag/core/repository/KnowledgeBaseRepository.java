package com.myrag.rag.core.repository;

import com.myrag.rag.core.entity.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseEntity, String> {
    List<KnowledgeBaseEntity> findByStatus(String status);
    Optional<KnowledgeBaseEntity> findByCollectionName(String collectionName);
}
