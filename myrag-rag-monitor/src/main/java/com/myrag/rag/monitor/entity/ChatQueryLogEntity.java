package com.myrag.rag.monitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_query_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryLogEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String query;

    @Column(name = "reply_preview", columnDefinition = "TEXT")
    private String replyPreview;

    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit;

    @Column(name = "need_rag")
    private Boolean needRag;

    @Column(name = "used_rag")
    private Boolean usedRag;

    @Column(name = "rag_kb_ids", columnDefinition = "TEXT")
    private String ragKbIds;

    @Column(name = "route_reason", columnDefinition = "TEXT")
    private String routeReason;

    @Column(name = "route_confidence")
    private Double routeConfidence;

    @Column(name = "recall_count")
    private Integer recallCount;

    @Column(name = "top_scores_json", columnDefinition = "TEXT")
    private String topScoresJson;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "quality_reason", columnDefinition = "TEXT")
    private String qualityReason;

    @Column(name = "latency_ms")
    private long latencyMs;

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
