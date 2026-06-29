package com.myrag.rag.monitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recall_quality_alert")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallQualityAlertEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "recall_log_id", length = 36)
    private String recallLogId;

    @Column(name = "kb_id", length = 36)
    private String kbId;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(name = "quality_score")
    private double qualityScore;

    @Column(columnDefinition = "TEXT")
    private String reason;

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
