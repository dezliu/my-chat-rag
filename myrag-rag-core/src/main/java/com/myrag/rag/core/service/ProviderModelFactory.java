package com.myrag.rag.core.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

interface ProviderModelFactory {

    ChatModel routerChatModel();

    ChatModel chatChatModel();

    EmbeddingModel embeddingModel();
}
