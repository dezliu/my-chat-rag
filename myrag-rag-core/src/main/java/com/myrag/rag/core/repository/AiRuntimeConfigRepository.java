package com.myrag.rag.core.repository;

import com.myrag.rag.core.entity.AiRuntimeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRuntimeConfigRepository extends JpaRepository<AiRuntimeConfigEntity, String> {
}
