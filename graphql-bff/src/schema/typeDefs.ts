export const typeDefs = `#graphql
  scalar JSON

  type Query {
    health: String!
    knowledgeBases: [KnowledgeBase!]!
    activeKnowledgeBases: [KnowledgeBase!]!
    documents(kbId: ID!): [Document!]!
    systemPrompt: SystemPrompt!
    aiConfig: AiConfig!
    monitorMetrics: JSON!
    recallLogs(kbId: ID, page: Int = 0, size: Int = 20): RecallLogPage!
    chatLogs(page: Int = 0, size: Int = 20): ChatLogPage!
    cacheLogs(page: Int = 0, size: Int = 20): CacheLogPage!
    alerts: [RecallAlert!]!
    ragSearch(input: RagSearchInput!): RagSearchResponse!
  }

  type Mutation {
    createKnowledgeBase(input: CreateKnowledgeBaseInput!): KnowledgeBase!
    updateKnowledgeBase(id: ID!, input: UpdateKnowledgeBaseInput!): KnowledgeBase!
    deleteKnowledgeBase(id: ID!): Boolean!
    deleteDocument(docId: ID!): Boolean!
    reindexDocument(docId: ID!): Document!
    updateSystemPrompt(prompt: String!): SystemPrompt!
    updateAiConfig(input: AiConfigUpdateInput!): AiConfig!
    recallTest(input: RagSearchInput!): RagSearchResponse!
    clearCache: ClearCacheResult!
    chat(input: ChatInput!): ChatResponse!
  }

  type KnowledgeBase {
    id: ID!
    name: String!
    description: String
    collectionName: String!
    status: String!
    chunkConfigJson: String
    createdAt: String!
  }

  type Document {
    id: ID!
    kbId: ID!
    filename: String!
    contentHash: String
    status: String!
    chunkCount: Int!
    createdAt: String!
  }

  type SystemPrompt {
    prompt: String!
  }

  type AiConfig {
    provider: String!
    baseUrl: String
    embeddingDimensions: Int
    apiKeyMasked: String!
    apiKeyConfigured: Boolean!
    apiKeySource: String!
    routerModel: String!
    chatModel: String!
    embeddingModel: String!
    customChatModels: [String!]!
    customRouterModels: [String!]!
  }

  input CreateKnowledgeBaseInput {
    name: String!
    description: String
    chunkConfigJson: String
  }

  input UpdateKnowledgeBaseInput {
    name: String
    description: String
    chunkConfigJson: String
  }

  input AiConfigUpdateInput {
    provider: String
    baseUrl: String
    embeddingDimensions: Int
    apiKey: String
    routerModel: String!
    chatModel: String!
    embeddingModel: String!
    customChatModels: [String!]
    customRouterModels: [String!]
  }

  input RagSearchInput {
    kbIds: [ID!]!
    query: String!
    topK: Int = 5
    minScore: Float = 0
  }

  type RagSearchResult {
    content: String!
    score: Float!
    docId: String
    kbId: String
    metadata: JSON
  }

  type RagSearchResponse {
    query: String!
    results: [RagSearchResult!]!
    latencyMs: Int!
  }

  type RecallLogPage {
    content: [RecallLog!]!
    totalElements: Int!
    totalPages: Int!
    size: Int!
    number: Int!
  }

  type ChatLogPage {
    content: [ChatLog!]!
    totalElements: Int!
    totalPages: Int!
    size: Int!
    number: Int!
  }

  type CacheLogPage {
    content: [CacheLog!]!
    totalElements: Int!
    totalPages: Int!
    size: Int!
    number: Int!
  }

  type RecallLog {
    id: ID!
    kbId: String
    query: String
    resultCount: Int
    latencyMs: Int
    createdAt: String
  }

  type ChatLog {
    id: ID!
    sessionId: String
    query: String
    replyPreview: String
    cacheHit: Boolean
    needRag: Boolean
    usedRag: Boolean
    ragKbIds: String
    routeReason: String
    routeConfidence: Float
    recallCount: Int
    qualityScore: Float
    qualityReason: String
    latencyMs: Int
    createdAt: String
  }

  type CacheLog {
    id: ID!
    query: String
    hit: Boolean
    usedRag: Boolean
    latencyMs: Int
    createdAt: String
  }

  type RecallAlert {
    id: ID!
    kbId: String
    query: String
    qualityScore: Float
    reason: String
    createdAt: String
  }

  type ClearCacheResult {
    deletedKeys: Int!
  }

  input ChatInput {
    sessionId: String!
    message: String!
  }

  type ChatResponse {
    sessionId: String!
    reply: String!
    usedRag: Boolean!
    ragKbIds: [String!]!
    routeDecision: RouteDecision
    cacheHit: Boolean!
  }

  type RouteDecision {
    needRag: Boolean!
    kbIds: [String!]!
    reason: String
    confidence: Float
  }
`;
