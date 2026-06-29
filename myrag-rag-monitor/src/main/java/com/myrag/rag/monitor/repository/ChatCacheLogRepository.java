package com.myrag.rag.monitor.repository;

import com.myrag.rag.monitor.entity.ChatCacheLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatCacheLogRepository extends JpaRepository<ChatCacheLogEntity, String> {
    Page<ChatCacheLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
