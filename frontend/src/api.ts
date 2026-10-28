const isLocalDev = typeof window !== 'undefined' && window.location.hostname === 'localhost' && window.location.port === '5173';
const API_BASE = import.meta.env.VITE_API_URL || (isLocalDev ? '/api' : 'https://expensesagenticai-backend.onrender.com/api');

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface Conversation {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: number;
  sender: 'USER' | 'AI';
  message: string;
  createdAt: string;
}

export interface Expense {
  id: number;
  amount: number;
  category: string;
  description: string;
  expenseDate: string;
  createdAt?: string;
}

export interface ExpenseSummary {
  startDate: string;
  endDate: string;
  totalAmount: number;
  transactionCount: number;
  categoryBreakdown: Record<string, number>;
}

export interface ChatResponse {
  conversationId: number;
  conversationTitle?: string;
  userMessage: Message;
  assistantMessage: Message;
  intent: string;
  details: Record<string, any>;
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${url}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
      },
      credentials: 'include', // sends and stores session cookies
    });
  } catch (err: any) {
    throw new Error('Unable to connect to server. Please check your network connection.');
  }

  // Read response body stream ONCE as text so it cannot be read twice
  const rawText = await res.text();
  let parsedData: any = null;
  if (rawText) {
    try {
      parsedData = JSON.parse(rawText);
    } catch {
      parsedData = null;
    }
  }

  if (!res.ok) {
    let errorMsg = parsedData?.message || parsedData?.error;
    if (!errorMsg) {
      if (res.status === 502 || res.status === 503 || res.status === 504) {
        errorMsg = `Backend server is temporarily unavailable (HTTP ${res.status}). If hosted on Render free tier, it may be waking up from sleep. Please wait a minute and try again.`;
      } else if (rawText && rawText.length < 150 && !rawText.includes('<html') && !rawText.includes('<!DOCTYPE')) {
        errorMsg = rawText;
      } else {
        errorMsg = res.statusText
          ? `${res.statusText} (${res.status})`
          : `Request failed with status ${res.status}`;
      }
    }
    throw new Error(errorMsg);
  }

  return (parsedData ?? ({} as T)) as T;
}

export const api = {
  // Auth
  async login(email: string, password: string): Promise<User> {
    const res = await request<{ id: number; name: string; email: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    return { id: res.id, name: res.name, email: res.email };
  },

  async register(name: string, email: string, password: string): Promise<User> {
    const res = await request<{ id: number; name: string; email: string }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, password }),
    });
    return { id: res.id, name: res.name, email: res.email };
  },

  async logout(): Promise<void> {
    await request('/auth/logout', { method: 'POST' });
  },

  async getMe(): Promise<User> {
    return request<User>('/users/me');
  },

  // Conversations
  async getConversations(): Promise<Conversation[]> {
    return request<Conversation[]>('/conversations');
  },

  async createConversation(title?: string): Promise<Conversation> {
    return request<Conversation>('/conversations', {
      method: 'POST',
      body: JSON.stringify({ title: title || 'New Chat' }),
    });
  },

  // Messages
  async getMessages(conversationId: number): Promise<Message[]> {
    return request<Message[]>(`/conversations/${conversationId}/messages`);
  },

  async sendMessage(conversationId: number, message: string): Promise<ChatResponse> {
    return request<ChatResponse>(`/conversations/${conversationId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ sender: 'USER', message }),
    });
  },

  // Expenses
  async getExpenses(startDate: string, endDate: string, category?: string): Promise<Expense[]> {
    let url = `/expenses?startDate=${startDate}&endDate=${endDate}`;
    if (category && category !== 'ALL') {
      url += `&category=${encodeURIComponent(category)}`;
    }
    return request<Expense[]>(url);
  },

  async getExpenseSummary(startDate: string, endDate: string): Promise<ExpenseSummary> {
    return request<ExpenseSummary>(`/expenses/summary?startDate=${startDate}&endDate=${endDate}`);
  },

  async createExpense(amount: number, category: string, description: string, expenseDate: string): Promise<Expense> {
    return request<Expense>('/expenses', {
      method: 'POST',
      body: JSON.stringify({ amount, category, description, expenseDate }),
    });
  },

  async deleteExpense(id: number): Promise<void> {
    await request(`/expenses/${id}`, { method: 'DELETE' });
  },
};
