import { GraphQLScalarType, Kind } from 'graphql';
import type { RestClient } from '../datasources/restClient.js';

const JSONScalar = new GraphQLScalarType({
  name: 'JSON',
  description: 'Arbitrary JSON value',
  serialize(value) {
    return value;
  },
  parseValue(value) {
    return value;
  },
  parseLiteral(ast) {
    if (ast.kind === Kind.STRING) {
      try {
        return JSON.parse(ast.value);
      } catch {
        return ast.value;
      }
    }
    return null;
  },
});

export function createResolvers(rest: RestClient) {
  return {
    JSON: JSONScalar,

    Query: {
      health: () => 'ok',
      knowledgeBases: () => rest.get('/api/v1/admin/knowledge-bases'),
      activeKnowledgeBases: () => rest.get('/api/v1/rag/knowledge-bases'),
      documents: (_: unknown, { kbId }: { kbId: string }) =>
        rest.get(`/api/v1/admin/knowledge-bases/${kbId}/documents`),
      systemPrompt: () => rest.get<{ prompt: string }>('/api/v1/admin/system-prompt'),
      aiConfig: () => rest.get('/api/v1/admin/ai-config'),
      monitorMetrics: () => rest.get('/api/v1/admin/monitor/metrics'),
      recallLogs: (_: unknown, args: { kbId?: string; page?: number; size?: number }) => {
        const params = new URLSearchParams({
          page: String(args.page ?? 0),
          size: String(args.size ?? 20),
        });
        if (args.kbId) params.set('kbId', args.kbId);
        return rest.get(`/api/v1/admin/monitor/recall-logs?${params}`);
      },
      chatLogs: (_: unknown, args: { page?: number; size?: number }) => {
        const params = new URLSearchParams({
          page: String(args.page ?? 0),
          size: String(args.size ?? 20),
        });
        return rest.get(`/api/v1/admin/monitor/chat-logs?${params}`);
      },
      cacheLogs: (_: unknown, args: { page?: number; size?: number }) => {
        const params = new URLSearchParams({
          page: String(args.page ?? 0),
          size: String(args.size ?? 20),
        });
        return rest.get(`/api/v1/admin/monitor/cache-logs?${params}`);
      },
      alerts: () => rest.get('/api/v1/admin/monitor/alerts'),
      ragSearch: (_: unknown, { input }: { input: Record<string, unknown> }) =>
        rest.post('/api/v1/rag/search', input),
    },

    Mutation: {
      createKnowledgeBase: (_: unknown, { input }: { input: Record<string, unknown> }) =>
        rest.post('/api/v1/admin/knowledge-bases', input),
      updateKnowledgeBase: (
        _: unknown,
        { id, input }: { id: string; input: Record<string, unknown> },
      ) => rest.put(`/api/v1/admin/knowledge-bases/${id}`, input),
      deleteKnowledgeBase: async (_: unknown, { id }: { id: string }) => {
        await rest.delete(`/api/v1/admin/knowledge-bases/${id}`);
        return true;
      },
      deleteDocument: async (_: unknown, { docId }: { docId: string }) => {
        await rest.delete(`/api/v1/admin/documents/${docId}`);
        return true;
      },
      reindexDocument: (_: unknown, { docId }: { docId: string }) =>
        rest.post(`/api/v1/admin/documents/${docId}/reindex`),
      updateSystemPrompt: (_: unknown, { prompt }: { prompt: string }) =>
        rest.put('/api/v1/admin/system-prompt', { prompt }),
      updateAiConfig: (_: unknown, { input }: { input: Record<string, unknown> }) =>
        rest.put('/api/v1/admin/ai-config', input),
      recallTest: (_: unknown, { input }: { input: Record<string, unknown> }) =>
        rest.post('/api/v1/admin/recall-test', input),
      clearCache: () => rest.delete<{ deletedKeys: number }>('/api/v1/admin/monitor/cache'),
      chat: (_: unknown, { input }: { input: Record<string, unknown> }) =>
        rest.post('/api/v1/chat', input),
    },
  };
}
