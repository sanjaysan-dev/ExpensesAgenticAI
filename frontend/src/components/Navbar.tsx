import React from 'react';
import { User } from '../api';
import { Sparkles, MessageSquare, BarChart3, LogOut, Menu } from 'lucide-react';

interface NavbarProps {
  user: User;
  activeTab: 'chat' | 'analytics';
  setActiveTab: (tab: 'chat' | 'analytics') => void;
  onLogout: () => void;
  onToggleSidebar?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  user,
  activeTab,
  setActiveTab,
  onLogout,
  onToggleSidebar,
}) => {
  return (
    <nav className="navbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        {activeTab === 'chat' && onToggleSidebar && (
          <button
            className="mobile-menu-btn"
            onClick={onToggleSidebar}
            aria-label="Toggle chat history"
          >
            <Menu size={22} />
          </button>
        )}
        <div className="brand">
          <div className="brand-icon">
            <Sparkles size={20} />
          </div>
          <span className="brand-text">ExpenseAI</span>
          <span className="ai-status-pill">
            <span className="pulse-dot" />
            <span className="ai-status-text">Agent Online</span>
          </span>
        </div>
      </div>

      <div className="nav-tabs">
        <button
          className={`nav-tab ${activeTab === 'chat' ? 'active' : ''}`}
          onClick={() => setActiveTab('chat')}
        >
          <MessageSquare size={16} />
          <span className="tab-label">AI Assistant</span>
        </button>

        <button
          className={`nav-tab ${activeTab === 'analytics' ? 'active' : ''}`}
          onClick={() => setActiveTab('analytics')}
        >
          <BarChart3 size={16} />
          <span className="tab-label">Analytics</span>
        </button>
      </div>

      <div className="user-profile">
        <div className="user-avatar" title={user.name}>
          {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
        </div>
        <div className="user-info">
          <span className="user-name">{user.name}</span>
          <span className="user-email">{user.email}</span>
        </div>
        <button onClick={onLogout} className="btn-logout" title="Log Out">
          <LogOut size={16} />
          <span className="logout-text">Logout</span>
        </button>
      </div>
    </nav>
  );
};
