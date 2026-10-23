import React, { useState, useEffect } from 'react';
import { api, User } from './api';
import { Auth } from './components/Auth';
import { Navbar } from './components/Navbar';
import { ChatView } from './components/ChatView';
import { AnalyticsView } from './components/AnalyticsView';

export const App: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'chat' | 'analytics'>('chat');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    try {
      const currentUser = await api.getMe();
      setUser(currentUser);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await api.logout();
    } catch (err) {
      console.error('Logout error', err);
    } finally {
      setUser(null);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh', background: 'var(--bg-main)' }}>
        <div style={{ textAlign: 'center' }}>
          <div className="spinner" style={{ borderColor: 'var(--primary)', borderTopColor: 'transparent', width: 32, height: 32, margin: '0 auto 16px' }} />
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Loading ExpenseAI Agent...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Auth onSuccess={(u) => setUser(u)} />;
  }

  return (
    <div className="app-container">
      <Navbar
        user={user}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onLogout={handleLogout}
        onToggleSidebar={() => setIsMobileSidebarOpen(!isMobileSidebarOpen)}
      />

      <div className="main-content">
        {activeTab === 'chat' ? (
          <ChatView
            isMobileSidebarOpen={isMobileSidebarOpen}
            onCloseMobileSidebar={() => setIsMobileSidebarOpen(false)}
            onExpenseAdded={() => setRefreshTrigger((prev) => prev + 1)}
          />
        ) : (
          <AnalyticsView key={refreshTrigger} />
        )}
      </div>
    </div>
  );
};
