package com.myrag.common.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

public interface DynamicModelProvider {

    ChatModel routerChatModel();

    ChatModel chatChatModel();

    EmbeddingModel embeddingModel();
}
