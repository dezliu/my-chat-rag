package com.myrag.rag.monitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recall_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallLogEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "kb_id", length = 36)
    private String kbId;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(name = "result_count")
    private int resultCount;

    @Column(name = "top_scores_json", columnDefinition = "TEXT")
    private String topScoresJson;

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
