package com.myrag.rag.monitor.repository;

import com.myrag.rag.monitor.entity.RecallQualityAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecallQualityAlertRepository extends JpaRepository<RecallQualityAlertEntity, String> {
    List<RecallQualityAlertEntity> findTop20ByOrderByCreatedAtDesc();
}
