'use client';

import type { ChatMessage } from '@/lib/types';

interface Props {
  message: ChatMessage;
}

export default function ChatMessageBubble({ message }: Props) {
  const isUser = message.role === 'user';

  return (
    <div className={`flex w-full ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[85%] rounded-2xl px-4 py-3 text-[15px] leading-relaxed shadow-sm ${
          isUser
            ? 'rounded-br-md bg-brand-600 text-white'
            : 'rounded-bl-md bg-white text-gray-800'
        }`}
      >
        <p className={`whitespace-pre-wrap break-words ${message.streaming ? 'cursor-blink' : ''}`}>
          {message.content || (message.streaming ? '' : '...')}
        </p>
        {!isUser && message.usedRag && !message.streaming && (
          <span className="mt-2 inline-block rounded-full bg-brand-50 px-2 py-0.5 text-xs text-brand-600">
            已引用知识库
          </span>
        )}
      </div>
    </div>
  );
}
