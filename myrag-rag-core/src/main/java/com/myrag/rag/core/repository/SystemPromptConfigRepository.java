package com.myrag.rag.core.repository;

import com.myrag.rag.core.entity.SystemPromptConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemPromptConfigRepository extends JpaRepository<SystemPromptConfigEntity, String> {
    Optional<SystemPromptConfigEntity> findTopByOrderByVersionDesc();
}
