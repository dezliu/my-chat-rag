package com.myrag.rag.core.repository;

import com.myrag.rag.core.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {
    List<DocumentEntity> findByKbId(String kbId);
}
