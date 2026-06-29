const SESSION_KEY = 'myrag_chat_session_id';

export function getOrCreateSessionId(): string {
  if (typeof window === 'undefined') {
    return '';
  }
  let sessionId = localStorage.getItem(SESSION_KEY);
  if (!sessionId) {
    sessionId = crypto.randomUUID();
    localStorage.setItem(SESSION_KEY, sessionId);
  }
  return sessionId;
}

export function resetSessionId(): string {
  const sessionId = crypto.randomUUID();
  localStorage.setItem(SESSION_KEY, sessionId);
  return sessionId;
}
