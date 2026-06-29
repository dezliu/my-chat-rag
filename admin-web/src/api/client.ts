import axios from 'axios'
import { apolloClient } from '../apollo/client'
import {
  AI_CONFIG_QUERY,
  ALERTS_QUERY,
  CACHE_LOGS_QUERY,
  CHAT_LOGS_QUERY,
  CLEAR_CACHE_MUTATION,
  CREATE_KB_MUTATION,
  DELETE_DOCUMENT_MUTATION,
  DELETE_KB_MUTATION,
  DOCUMENTS_QUERY,
  KNOWLEDGE_BASES_QUERY,
  MONITOR_METRICS_QUERY,
  RECALL_LOGS_QUERY,
  RECALL_TEST_MUTATION,
  SYSTEM_PROMPT_QUERY,
  UPDATE_AI_CONFIG_MUTATION,
  UPDATE_SYSTEM_PROMPT_MUTATION,
} from '../graphql/operations'

const uploadApi = axios.create({ baseURL: '/api/v1' })

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface KnowledgeBase {
  id: string
  name: string
  description: string
  collectionName: string
  status: string
  chunkConfigJson: string
  createdAt: string
}

export interface Document {
  id: string
  kbId: string
  filename: string
  contentHash: string
  status: string
  chunkCount: number
  createdAt: string
}

export interface RagSearchResult {
  content: string
  score: number
  docId: string
  kbId: string
}

export interface RagSearchResponse {
  query: string
  results: RagSearchResult[]
  latencyMs: number
}

export interface AiConfig {
  provider: 'dashscope' | 'zhipuai'
  baseUrl?: string
  embeddingDimensions: number
  apiKeyMasked: string
  apiKeyConfigured: boolean
  apiKeySource: 'db' | 'env'
  routerModel: string
  chatModel: string
  embeddingModel: string
  customChatModels: string[]
  customRouterModels: string[]
}

export interface AiConfigUpdate {
  provider?: 'dashscope' | 'zhipuai'
  baseUrl?: string
  embeddingDimensions?: number
  apiKey?: string
  routerModel: string
  chatModel: string
  embeddingModel: string
  customChatModels?: string[]
  customRouterModels?: string[]
}

function ok<T>(data: T): { data: ApiResponse<T> } {
  return { data: { code: 0, message: 'success', data } }
}

export const adminApi = {
  listKnowledgeBases: async () => {
    const result = await apolloClient.query<{ knowledgeBases: KnowledgeBase[] }>({
      query: KNOWLEDGE_BASES_QUERY,
    })
    return ok(result.data.knowledgeBases)
  },
  createKnowledgeBase: async (data: { name: string; description?: string }) => {
    const result = await apolloClient.mutate<{ createKnowledgeBase: KnowledgeBase }>({
      mutation: CREATE_KB_MUTATION,
      variables: { input: data },
    })
    return ok(result.data!.createKnowledgeBase)
  },
  deleteKnowledgeBase: async (id: string) => {
    await apolloClient.mutate({ mutation: DELETE_KB_MUTATION, variables: { id } })
    return ok(null)
  },
  listDocuments: async (kbId: string) => {
    const result = await apolloClient.query<{ documents: Document[] }>({
      query: DOCUMENTS_QUERY,
      variables: { kbId },
    })
    return ok(result.data.documents)
  },
  uploadDocument: (kbId: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return uploadApi.post<ApiResponse<Document>>(`/admin/knowledge-bases/${kbId}/documents`, form)
  },
  deleteDocument: async (docId: string) => {
    await apolloClient.mutate({ mutation: DELETE_DOCUMENT_MUTATION, variables: { docId } })
    return ok(null)
  },
  getSystemPrompt: async () => {
    const result = await apolloClient.query<{ systemPrompt: { prompt: string } }>({
      query: SYSTEM_PROMPT_QUERY,
    })
    return ok(result.data.systemPrompt)
  },
  updateSystemPrompt: async (prompt: string) => {
    const result = await apolloClient.mutate<{ updateSystemPrompt: { prompt: string } }>({
      mutation: UPDATE_SYSTEM_PROMPT_MUTATION,
      variables: { prompt },
    })
    return ok(result.data!.updateSystemPrompt)
  },
  getAiConfig: async () => {
    const result = await apolloClient.query<{ aiConfig: AiConfig }>({
      query: AI_CONFIG_QUERY,
    })
    return ok(result.data.aiConfig)
  },
  updateAiConfig: async (data: AiConfigUpdate) => {
    const result = await apolloClient.mutate<{ updateAiConfig: AiConfig }>({
      mutation: UPDATE_AI_CONFIG_MUTATION,
      variables: { input: data },
    })
    return ok(result.data!.updateAiConfig)
  },
  recallTest: async (data: { kbIds: string[]; query: string; topK?: number }) => {
    const result = await apolloClient.mutate<{ recallTest: RagSearchResponse }>({
      mutation: RECALL_TEST_MUTATION,
      variables: { input: data },
    })
    return ok(result.data!.recallTest)
  },
  getMetrics: async () => {
    const result = await apolloClient.query<{ monitorMetrics: Record<string, number> }>({
      query: MONITOR_METRICS_QUERY,
    })
    return ok(result.data.monitorMetrics)
  },
  getRecallLogs: async (page = 0, size = 20) => {
    const result = await apolloClient.query({
      query: RECALL_LOGS_QUERY,
      variables: { page, size },
    })
    return ok(result.data.recallLogs)
  },
  getCacheLogs: async (page = 0, size = 20) => {
    const result = await apolloClient.query({
      query: CACHE_LOGS_QUERY,
      variables: { page, size },
    })
    return ok(result.data.cacheLogs)
  },
  getChatLogs: async (page = 0, size = 20) => {
    const result = await apolloClient.query({
      query: CHAT_LOGS_QUERY,
      variables: { page, size },
    })
    return ok(result.data.chatLogs)
  },
  clearCache: async () => {
    const result = await apolloClient.mutate<{ clearCache: { deletedKeys: number } }>({
      mutation: CLEAR_CACHE_MUTATION,
    })
    return ok(result.data!.clearCache)
  },
  getAlerts: async () => {
    const result = await apolloClient.query({
      query: ALERTS_QUERY,
    })
    return ok(result.data.alerts)
  },
}

export default uploadApi
