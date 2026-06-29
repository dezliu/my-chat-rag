package com.myrag.rag.monitor.repository;

import com.myrag.rag.monitor.entity.ChatQueryLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatQueryLogRepository extends JpaRepository<ChatQueryLogEntity, String> {
    Page<ChatQueryLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
