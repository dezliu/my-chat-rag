'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { chat, chatStream } from '@/lib/api';
import { getOrCreateSessionId, resetSessionId } from '@/lib/session';
import type { ChatMessage } from '@/lib/types';
import ChatInput from './ChatInput';
import ChatMessageBubble from './ChatMessage';

function createId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [loading, setLoading] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setSessionId(getOrCreateSessionId());
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleNewChat = () => {
    if (streaming) abortRef.current?.abort();
    setMessages([]);
    setInput('');
    setError(null);
    setSessionId(resetSessionId());
  };

  const updateAssistant = useCallback((id: string, patch: Partial<ChatMessage>) => {
    setMessages((prev) => prev.map((m) => (m.id === id ? { ...m, ...patch } : m)));
  }, []);

  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading || streaming || !sessionId) return;

    setError(null);
    setInput('');

    const userMsg: ChatMessage = { id: createId(), role: 'user', content: text };
    const assistantId = createId();
    const assistantMsg: ChatMessage = {
      id: assistantId,
      role: 'assistant',
      content: '',
      streaming: true,
    };

    setMessages((prev) => [...prev, userMsg, assistantMsg]);
    setStreaming(true);

    const controller = new AbortController();
    abortRef.current = controller;

    try {
      await chatStream(
        { sessionId, message: text },
        (chunk) => {
          setMessages((prev) =>
            prev.map((m) =>
              m.id === assistantId ? { ...m, content: m.content + chunk } : m,
            ),
          );
        },
        controller.signal,
      );
      updateAssistant(assistantId, { streaming: false });
    } catch (err) {
      if (controller.signal.aborted) {
        updateAssistant(assistantId, { streaming: false });
        return;
      }

      // 流式失败时降级为非流式
      try {
        setLoading(true);
        const data = await chat({ sessionId, message: text });
        updateAssistant(assistantId, {
          content: data.reply,
          usedRag: data.usedRag,
          streaming: false,
        });
      } catch (fallbackErr) {
        const msg = fallbackErr instanceof Error ? fallbackErr.message : '发送失败';
        setError(msg);
        setMessages((prev) => prev.filter((m) => m.id !== assistantId));
      } finally {
        setLoading(false);
      }
    } finally {
      setStreaming(false);
      abortRef.current = null;
    }
  };

  const handleStop = () => {
    abortRef.current?.abort();
    setStreaming(false);
    setMessages((prev) =>
      prev.map((m) => (m.streaming ? { ...m, streaming: false } : m)),
    );
  };

  return (
    <div className="flex h-[100dvh] flex-col bg-[#f5f7fb]">
      {/* Header */}
      <header className="flex shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4 py-3 pt-[max(12px,env(safe-area-inset-top))]">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">MyRAG 智能助手</h1>
          <p className="text-xs text-gray-400">自动检索知识库 · 智能回答</p>
        </div>
        <button
          type="button"
          onClick={handleNewChat}
          className="rounded-full border border-gray-200 px-3 py-1.5 text-sm text-gray-600 active:bg-gray-50"
        >
          新对话
        </button>
      </header>

      {/* Messages */}
      <main className="chat-scroll flex-1 overflow-y-auto px-4 py-4">
        {messages.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center text-center text-gray-400">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-brand-100 text-2xl">
              💬
            </div>
            <p className="text-base font-medium text-gray-600">有什么可以帮你的？</p>
            <p className="mt-2 max-w-xs text-sm">
              我会自动判断是否需要检索知识库，并基于相关内容为你回答
            </p>
          </div>
        )}

        <div className="flex flex-col gap-4">
          {messages.map((msg) => (
            <ChatMessageBubble key={msg.id} message={msg} />
          ))}
        </div>
        <div ref={bottomRef} />
      </main>

      {error && (
        <div className="mx-4 mb-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
          {error}
        </div>
      )}

      <ChatInput
        value={input}
        onChange={setInput}
        onSend={handleSend}
        onStop={handleStop}
        disabled={loading}
        streaming={streaming}
      />
    </div>
  );
}
