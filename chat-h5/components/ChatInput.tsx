'use client';

interface Props {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  onStop?: () => void;
  disabled?: boolean;
  streaming?: boolean;
}

export default function ChatInput({ value, onChange, onSend, onStop, disabled, streaming }: Props) {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!disabled && value.trim()) onSend();
    }
  };

  return (
    <div
      className="border-t border-gray-200 bg-white px-4 py-3"
      style={{ paddingBottom: 'calc(12px + var(--safe-bottom))' }}
    >
      <div className="flex items-end gap-2">
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入你的问题..."
          rows={1}
          disabled={disabled}
          className="max-h-32 min-h-[44px] flex-1 resize-none rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-[15px] outline-none focus:border-brand-500 focus:bg-white disabled:opacity-50"
        />
        {streaming ? (
          <button
            type="button"
            onClick={onStop}
            className="flex h-11 min-w-[56px] items-center justify-center rounded-2xl bg-red-500 px-4 text-sm font-medium text-white"
          >
            停止
          </button>
        ) : (
          <button
            type="button"
            onClick={onSend}
            disabled={disabled || !value.trim()}
            className="flex h-11 min-w-[56px] items-center justify-center rounded-2xl bg-brand-600 px-4 text-sm font-medium text-white disabled:bg-gray-300"
          >
            发送
          </button>
        )}
      </div>
      <p className="mt-2 text-center text-xs text-gray-400">Enter 发送 · Shift+Enter 换行</p>
    </div>
  );
}
