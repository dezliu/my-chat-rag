export type MessageRole = 'user' | 'assistant';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  content: string;
  usedRag?: boolean;
  streaming?: boolean;
}

export interface ChatRequest {
  sessionId: string;
  message: string;
}

export interface RagRouteDecision {
  needRag: boolean;
  kbIds: string[];
  reason: string;
  confidence: number;
}

export interface ChatResponseData {
  sessionId: string;
  reply: string;
  usedRag: boolean;
  ragKbIds: string[];
  routeDecision?: RagRouteDecision;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
