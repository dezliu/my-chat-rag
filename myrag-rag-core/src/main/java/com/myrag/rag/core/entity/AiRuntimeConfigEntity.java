package com.myrag.rag.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ai_runtime_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRuntimeConfigEntity {

    public static final String DEFAULT_ID = "default";

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "router_model", length = 100)
    private String routerModel;

    @Column(name = "chat_model", length = 100)
    private String chatModel;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "custom_chat_models_json", columnDefinition = "TEXT")
    private String customChatModelsJson;

    @Column(name = "custom_router_models_json", columnDefinition = "TEXT")
    private String customRouterModelsJson;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = DEFAULT_ID;
        }
    }
}
