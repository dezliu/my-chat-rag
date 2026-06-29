package com.myrag.rag.core.repository;

import com.myrag.rag.core.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, String> {
    List<DocumentChunkEntity> findByDocId(String docId);
    void deleteByDocId(String docId);
}
