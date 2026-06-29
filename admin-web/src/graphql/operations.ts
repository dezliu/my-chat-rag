import { gql } from '@apollo/client';

export const KNOWLEDGE_BASES_QUERY = gql`
  query KnowledgeBases {
    knowledgeBases {
      id
      name
      description
      collectionName
      status
      chunkConfigJson
      createdAt
    }
  }
`;

export const CREATE_KB_MUTATION = gql`
  mutation CreateKnowledgeBase($input: CreateKnowledgeBaseInput!) {
    createKnowledgeBase(input: $input) {
      id
      name
      description
      collectionName
      status
      createdAt
    }
  }
`;

export const DELETE_KB_MUTATION = gql`
  mutation DeleteKnowledgeBase($id: ID!) {
    deleteKnowledgeBase(id: $id)
  }
`;

export const DOCUMENTS_QUERY = gql`
  query Documents($kbId: ID!) {
    documents(kbId: $kbId) {
      id
      kbId
      filename
      contentHash
      status
      chunkCount
      createdAt
    }
  }
`;

export const DELETE_DOCUMENT_MUTATION = gql`
  mutation DeleteDocument($docId: ID!) {
    deleteDocument(docId: $docId)
  }
`;

export const SYSTEM_PROMPT_QUERY = gql`
  query SystemPrompt {
    systemPrompt {
      prompt
    }
  }
`;

export const UPDATE_SYSTEM_PROMPT_MUTATION = gql`
  mutation UpdateSystemPrompt($prompt: String!) {
    updateSystemPrompt(prompt: $prompt) {
      prompt
    }
  }
`;

export const AI_CONFIG_QUERY = gql`
  query AiConfig {
    aiConfig {
      provider
      baseUrl
      embeddingDimensions
      apiKeyMasked
      apiKeyConfigured
      apiKeySource
      routerModel
      chatModel
      embeddingModel
      customChatModels
      customRouterModels
    }
  }
`;

export const UPDATE_AI_CONFIG_MUTATION = gql`
  mutation UpdateAiConfig($input: AiConfigUpdateInput!) {
    updateAiConfig(input: $input) {
      provider
      baseUrl
      embeddingDimensions
      apiKeyMasked
      apiKeyConfigured
      apiKeySource
      routerModel
      chatModel
      embeddingModel
      customChatModels
      customRouterModels
    }
  }
`;

export const RECALL_TEST_MUTATION = gql`
  mutation RecallTest($input: RagSearchInput!) {
    recallTest(input: $input) {
      query
      latencyMs
      results {
        content
        score
        docId
        kbId
      }
    }
  }
`;

export const MONITOR_METRICS_QUERY = gql`
  query MonitorMetrics {
    monitorMetrics
  }
`;

export const RECALL_LOGS_QUERY = gql`
  query RecallLogs($page: Int, $size: Int) {
    recallLogs(page: $page, size: $size) {
      content {
        id
        kbId
        query
        resultCount
        latencyMs
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const CACHE_LOGS_QUERY = gql`
  query CacheLogs($page: Int, $size: Int) {
    cacheLogs(page: $page, size: $size) {
      content {
        id
        query
        hit
        usedRag
        latencyMs
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const CHAT_LOGS_QUERY = gql`
  query ChatLogs($page: Int, $size: Int) {
    chatLogs(page: $page, size: $size) {
      content {
        id
        sessionId
        query
        replyPreview
        cacheHit
        needRag
        usedRag
        ragKbIds
        routeReason
        routeConfidence
        recallCount
        qualityScore
        qualityReason
        latencyMs
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const ALERTS_QUERY = gql`
  query Alerts {
    alerts {
      id
      kbId
      query
      qualityScore
      reason
      createdAt
    }
  }
`;

export const CLEAR_CACHE_MUTATION = gql`
  mutation ClearCache {
    clearCache {
      deletedKeys
    }
  }
`;
