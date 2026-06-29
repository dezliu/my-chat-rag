package com.myrag.rag.monitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_cache_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCacheLogEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false)
    private boolean hit;

    @Column(name = "used_rag")
    private Boolean usedRag;

    @Column(name = "kb_ids", columnDefinition = "TEXT")
    private String kbIds;

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
