import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

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

export const adminApi = {
  listKnowledgeBases: () => api.get<ApiResponse<KnowledgeBase[]>>('/admin/knowledge-bases'),
  createKnowledgeBase: (data: { name: string; description?: string }) =>
    api.post<ApiResponse<KnowledgeBase>>('/admin/knowledge-bases', data),
  deleteKnowledgeBase: (id: string) => api.delete(`/admin/knowledge-bases/${id}`),
  listDocuments: (kbId: string) => api.get<ApiResponse<Document[]>>(`/admin/knowledge-bases/${kbId}/documents`),
  uploadDocument: (kbId: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.post<ApiResponse<Document>>(`/admin/knowledge-bases/${kbId}/documents`, form)
  },
  deleteDocument: (docId: string) => api.delete(`/admin/documents/${docId}`),
  getSystemPrompt: () => api.get<ApiResponse<{ prompt: string }>>('/admin/system-prompt'),
  updateSystemPrompt: (prompt: string) => api.put('/admin/system-prompt', { prompt }),
  recallTest: (data: { kbIds: string[]; query: string; topK?: number }) =>
    api.post<ApiResponse<RagSearchResponse>>('/admin/recall-test', data),
  getMetrics: () => api.get<ApiResponse<Record<string, number>>>('/admin/monitor/metrics'),
  getRecallLogs: (page = 0, size = 20) =>
    api.get(`/admin/monitor/recall-logs?page=${page}&size=${size}`),
  getAlerts: () => api.get('/admin/monitor/alerts'),
}

export default api
