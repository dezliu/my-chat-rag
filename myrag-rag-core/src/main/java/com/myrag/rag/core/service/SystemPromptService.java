package com.myrag.rag.core.service;

import com.myrag.common.config.ChatProperties;
import com.myrag.rag.core.entity.SystemPromptConfigEntity;
import com.myrag.rag.core.repository.SystemPromptConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemPromptService {

    private final SystemPromptConfigRepository repository;
    private final ChatProperties chatProperties;

    public String getActivePrompt() {
        return repository.findTopByOrderByVersionDesc()
                .map(SystemPromptConfigEntity::getPromptText)
                .orElse(chatProperties.getDefaultSystemPrompt());
    }

    @Transactional
    public String updatePrompt(String promptText) {
        int nextVersion = repository.findTopByOrderByVersionDesc()
                .map(e -> e.getVersion() + 1)
                .orElse(1);
        SystemPromptConfigEntity entity = SystemPromptConfigEntity.builder()
                .promptText(promptText)
                .version(nextVersion)
                .build();
        repository.save(entity);
        return promptText;
    }
}
