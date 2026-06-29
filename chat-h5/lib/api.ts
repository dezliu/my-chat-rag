import type { ChatRequest, ChatResponseData } from './types';

const CHAT_MUTATION = `
  mutation Chat($input: ChatInput!) {
    chat(input: $input) {
      sessionId
      reply
      usedRag
      ragKbIds
      cacheHit
      routeDecision {
        needRag
        kbIds
        reason
        confidence
      }
    }
  }
`;

export async function chat(request: ChatRequest): Promise<ChatResponseData> {
  const res = await fetch('/graphql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: CHAT_MUTATION,
      variables: { input: request },
    }),
  });
  if (!res.ok) {
    throw new Error(`GraphQL 请求失败: ${res.status}`);
  }
  const json = await res.json();
  if (json.errors?.length) {
    throw new Error(json.errors[0].message || '对话失败');
  }
  return json.data.chat as ChatResponseData;
}

export async function chatStream(
  request: ChatRequest,
  onChunk: (chunk: string) => void,
  signal?: AbortSignal,
): Promise<void> {
  const res = await fetch('/api/v1/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(request),
    signal,
  });

  if (!res.ok) {
    throw new Error(`流式请求失败: ${res.status}`);
  }

  const reader = res.body?.getReader();
  if (!reader) {
    throw new Error('浏览器不支持流式响应');
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) continue;
      if (trimmed.startsWith('data:')) {
        const data = trimmed.slice(5).trim();
        if (data && data !== '[DONE]') {
          onChunk(data);
        }
      } else {
        onChunk(trimmed);
      }
    }
  }

  if (buffer.trim()) {
    const trimmed = buffer.trim();
    if (trimmed.startsWith('data:')) {
      const data = trimmed.slice(5).trim();
      if (data && data !== '[DONE]') onChunk(data);
    } else {
      onChunk(trimmed);
    }
  }
}
