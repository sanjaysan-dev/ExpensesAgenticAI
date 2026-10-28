import React, { useState, useEffect, useRef } from 'react';
import { api, Conversation, Message } from '../api';
import { Plus, Send, Bot, User as UserIcon, MessageSquare, ArrowUpRight, AlertCircle, Trash2, X } from 'lucide-react';

interface ChatViewProps {
  onExpenseAdded?: () => void;
  isMobileSidebarOpen?: boolean;
  onCloseMobileSidebar?: () => void;
}

export const ChatView: React.FC<ChatViewProps> = ({
  onExpenseAdded,
  isMobileSidebarOpen = false,
  onCloseMobileSidebar,
}) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [hiddenConvIds, setHiddenConvIds] = useState<number[]>(() => {
    try {
      const stored = localStorage.getItem('hidden_conv_ids');
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  });

  const [activeConvId, setActiveConvId] = useState<number | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingConv, setLoadingConv] = useState(false);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const suggestions = [
    'i spend 300 for today dinner',
    'Paid ₹120 for bus fare',
    'Bought groceries for Rs. 800 yesterday',
    'Remove that dinner amount',
    'How much did I spend yesterday?',
    'Show me my expenses this month',
  ];

  useEffect(() => {
    loadConversations();
  }, []);

  useEffect(() => {
    if (activeConvId) {
      loadMessages(activeConvId);
    }
  }, [activeConvId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const visibleConversations = conversations.filter((c) => !hiddenConvIds.includes(c.id));

  const loadConversations = async () => {
    try {
      setLoadingConv(true);
      setErrorBanner(null);
      const list = await api.getConversations();
      setConversations(list);

      const visible = list.filter((c) => !hiddenConvIds.includes(c.id));
      if (visible.length > 0) {
        setActiveConvId(visible[0].id);
      } else {
        const initialConv = await api.createConversation('New Chat');
        setConversations([initialConv]);
        setActiveConvId(initialConv.id);
      }
    } catch (err: any) {
      console.error('Failed to load conversations', err);
      setErrorBanner('Could not connect to conversation service: ' + (err.message || 'Error'));
    } finally {
      setLoadingConv(false);
    }
  };

  const loadMessages = async (convId: number) => {
    try {
      const msgs = await api.getMessages(convId);
      setMessages(msgs);
    } catch (err) {
      console.error('Failed to load messages', err);
    }
  };

  const handleNewChat = async () => {
    try {
      setLoading(true);
      setErrorBanner(null);
      const conv = await api.createConversation('New Chat');
      setConversations((prev) => [conv, ...prev]);
      setActiveConvId(conv.id);
      setMessages([]);
      if (onCloseMobileSidebar) onCloseMobileSidebar();
      setTimeout(() => inputRef.current?.focus(), 100);
    } catch (err: any) {
      console.error('Failed to create new conversation', err);
      setErrorBanner('Failed to create new chat: ' + (err.message || 'Error'));
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteChat = (convId: number, e: React.MouseEvent) => {
    e.stopPropagation();

    const newHidden = [...hiddenConvIds, convId];
    setHiddenConvIds(newHidden);
    try {
      localStorage.setItem('hidden_conv_ids', JSON.stringify(newHidden));
    } catch (err) {
      console.error('Failed to save hidden conv', err);
    }

    if (activeConvId === convId) {
      const remaining = conversations.filter((c) => !newHidden.includes(c.id));
      if (remaining.length > 0) {
        setActiveConvId(remaining[0].id);
      } else {
        setActiveConvId(null);
        setMessages([]);
      }
    }
  };

  const handleSend = async (textToSend?: string) => {
    const text = (textToSend || input).trim();
    if (!text || loading) return;

    setInput('');
    setErrorBanner(null);

    let targetConvId = activeConvId;

    if (!targetConvId) {
      try {
        setLoading(true);
        const newConv = await api.createConversation('New Chat');
        setConversations((prev) => [newConv, ...prev]);
        setActiveConvId(newConv.id);
        targetConvId = newConv.id;
      } catch (err: any) {
        setErrorBanner('Could not create conversation: ' + (err.message || 'Error'));
        setLoading(false);
        return;
      }
    }

    const tempUserMsg: Message = {
      id: Date.now(),
      sender: 'USER',
      message: text,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMsg]);
    setLoading(true);

    try {
      const response = await api.sendMessage(targetConvId, text);

      setMessages((prev) => [
        ...prev.filter((m) => m.id !== tempUserMsg.id),
        response.userMessage,
        response.assistantMessage,
      ]);

      if (response.conversationTitle) {
        setConversations((prev) =>
          prev.map((c) =>
            c.id === targetConvId ? { ...c, title: response.conversationTitle! } : c
          )
        );
      }

      if ((response.intent === 'RECORD_EXPENSE' || response.intent === 'DELETE_EXPENSE') && onExpenseAdded) {
        onExpenseAdded();
      }
    } catch (err: any) {
      const errorMsg: Message = {
        id: Date.now() + 1,
        sender: 'AI',
        message: `⚠️ Error processing request: ${err.message || 'Internal server error'}`,
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  const activeConv = visibleConversations.find((c) => c.id === activeConvId);

  // Render clean response message
  const renderMessageContent = (msg: Message) => (
    <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>{msg.message}</div>
  );

  return (
    <div className="chat-layout">
      {/* Mobile Drawer Backdrop */}
      {isMobileSidebarOpen && (
        <div
          className="sidebar-backdrop"
          onClick={onCloseMobileSidebar}
        />
      )}

      {/* Sidebar */}
      <div className={`sidebar ${isMobileSidebarOpen ? 'mobile-open' : ''}`}>
        <div className="sidebar-header">
          <button className="btn-new-chat" onClick={handleNewChat} disabled={loading}>
            <Plus size={18} />
            <span>New Chat</span>
          </button>
          {onCloseMobileSidebar && (
            <button className="mobile-close-btn" onClick={onCloseMobileSidebar}>
              <X size={20} />
            </button>
          )}
        </div>

        <div className="conversation-list">
          {loadingConv ? (
            <div style={{ padding: 16, textAlign: 'center', color: 'var(--text-muted)' }}>
              Loading chats...
            </div>
          ) : visibleConversations.length === 0 ? (
            <div style={{ padding: 16, textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              No chats visible. Start a new chat!
            </div>
          ) : (
            visibleConversations.map((conv) => (
              <div
                key={conv.id}
                className={`conversation-item ${conv.id === activeConvId ? 'active' : ''}`}
                onClick={() => {
                  setActiveConvId(conv.id);
                  if (onCloseMobileSidebar) onCloseMobileSidebar();
                }}
                style={{ justifyContent: 'space-between' }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, overflow: 'hidden', flex: 1 }}>
                  <MessageSquare size={16} style={{ flexShrink: 0 }} />
                  <span className="conversation-title">{conv.title}</span>
                </div>
                <button
                  type="button"
                  onClick={(e) => handleDeleteChat(conv.id, e)}
                  className="btn-delete-conv"
                  title="Remove from chat history (keeps database data)"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Chat Area */}
      <div className="chat-area">
        <div className="chat-header">
          <span className="chat-header-title">
            {activeConv ? activeConv.title : 'AI Expense Assistant'}
          </span>
        </div>

        {errorBanner && (
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '10px 24px',
            background: '#fee2e2', color: '#b91c1c', fontSize: '0.85rem'
          }}>
            <AlertCircle size={16} />
            <span>{errorBanner}</span>
          </div>
        )}

        <div className="chat-messages">
          {messages.length === 0 && (
            <div style={{ margin: 'auto', textAlign: 'center', maxWidth: 460, color: 'var(--text-muted)', padding: '20px 10px' }}>
              <div style={{
                display: 'inline-flex', padding: 16, background: 'var(--primary-light)',
                borderRadius: '50%', color: 'var(--primary)', marginBottom: 12
              }}>
                <Bot size={36} />
              </div>
              <h3 style={{ color: 'var(--text-main)', marginBottom: 8, fontSize: '1.25rem' }}>
                AI Expense Assistant Ready
              </h3>
              <p style={{ fontSize: '0.9rem', marginBottom: 20 }}>
                Tell me what you spent or ask any question about your spending.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {suggestions.map((sug, i) => (
                  <button
                    key={i}
                    onClick={() => handleSend(sug)}
                    className="suggestion-action-btn"
                  >
                    <span>{sug}</span>
                    <ArrowUpRight size={16} color="var(--primary)" />
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <div key={msg.id} className={`message-row ${msg.sender === 'USER' ? 'user' : 'ai'}`}>
              <div className={`avatar ${msg.sender === 'USER' ? 'user' : 'ai'}`}>
                {msg.sender === 'USER' ? <UserIcon size={18} /> : <Bot size={18} />}
              </div>
              <div className="message-bubble">
                {renderMessageContent(msg)}
                <div className="message-time">
                  {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </div>
              </div>
            </div>
          ))}

          {loading && (
            <div className="message-row ai">
              <div className="avatar ai">
                <Bot size={18} />
              </div>
              <div className="message-bubble" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div className="spinner" style={{ borderColor: 'var(--primary)', borderTopColor: 'transparent', width: 14, height: 14 }} />
                <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>AI Assistant is thinking...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Suggestion Chips */}
        {messages.length > 0 && (
          <div className="suggestions-bar">
            {suggestions.map((sug, i) => (
              <button key={i} className="suggestion-pill" onClick={() => handleSend(sug)}>
                {sug}
              </button>
            ))}
          </div>
        )}

        {/* Input Form */}
        <div className="chat-input-container">
          <form
            className="chat-input-form"
            onSubmit={(e) => {
              e.preventDefault();
              handleSend();
            }}
          >
            <input
              ref={inputRef}
              type="text"
              placeholder="e.g. i spend 300 for today dinner, or how much did i spend yesterday?"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              className="chat-input"
              disabled={loading}
              autoFocus
            />
            <button
              type="submit"
              disabled={!input.trim() || loading}
              className="btn-send"
              title="Send message"
            >
              <Send size={18} />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
