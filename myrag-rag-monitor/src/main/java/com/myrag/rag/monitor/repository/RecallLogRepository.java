package com.myrag.rag.monitor.repository;

import com.myrag.rag.monitor.entity.RecallLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecallLogRepository extends JpaRepository<RecallLogEntity, String> {
    Page<RecallLogEntity> findByKbId(String kbId, Pageable pageable);
}
