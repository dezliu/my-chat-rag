package com.myrag.rag.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunk")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunkEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "doc_id", nullable = false, length = 36)
    private String docId;

    @Column(name = "kb_id", nullable = false, length = 36)
    private String kbId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content_preview", columnDefinition = "TEXT")
    private String contentPreview;

    @Column(name = "qdrant_point_id", length = 100)
    private String qdrantPointId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
