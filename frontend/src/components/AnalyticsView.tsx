import React, { useState, useEffect } from 'react';
import { api, Expense, ExpenseSummary } from '../api';
import { IndianRupee, TrendingUp, Calendar, Tag, PieChart, Trash2 } from 'lucide-react';

export const AnalyticsView: React.FC = () => {
  const todayStr = new Date().toISOString().split('T')[0];
  const firstDayOfMonthStr = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];

  const [startDate, setStartDate] = useState(firstDayOfMonthStr);
  const [endDate, setEndDate] = useState(todayStr);
  const [category, setCategory] = useState('ALL');
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [summary, setSummary] = useState<ExpenseSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const categories = ['ALL', 'Food', 'Transport', 'Entertainment', 'Healthcare', 'Bills', 'Shopping', 'Other'];

  useEffect(() => {
    fetchData();
  }, [startDate, endDate, category]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [expList, sumData] = await Promise.all([
        api.getExpenses(startDate, endDate, category),
        api.getExpenseSummary(startDate, endDate),
      ]);
      setExpenses(expList);
      setSummary(sumData);
    } catch (err) {
      console.error('Failed to load expenses', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this expense?')) return;
    try {
      setDeletingId(id);
      await api.deleteExpense(id);
      await fetchData();
    } catch (err: any) {
      console.error('Failed to delete expense', err);
      alert('Could not delete expense: ' + (err.message || 'Error'));
    } finally {
      setDeletingId(null);
    }
  };

  const getBadgeClass = (cat: string) => {
    const key = cat.toLowerCase();
    if (key.includes('food')) return 'badge-food';
    if (key.includes('transport')) return 'badge-transport';
    if (key.includes('entertainment')) return 'badge-entertainment';
    if (key.includes('health')) return 'badge-healthcare';
    if (key.includes('bill')) return 'badge-bills';
    if (key.includes('shop')) return 'badge-shopping';
    return 'badge-other';
  };

  return (
    <div className="analytics-view">
      {/* Header & Filters */}
      <div className="analytics-header">
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 700 }}>Spending & Expense Analytics</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Live breakdown of expenses recorded by your AI Assistant
          </p>
        </div>

        <div className="filter-bar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Calendar size={16} color="var(--text-muted)" />
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="date-input"
            />
            <span style={{ color: 'var(--text-muted)' }}>to</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="date-input"
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginLeft: 8 }}>
            <Tag size={16} color="var(--text-muted)" />
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="select-input"
            >
              {categories.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ backgroundColor: '#e0e7ff', color: 'var(--primary)' }}>
            <IndianRupee size={24} />
          </div>
          <div>
            <div className="stat-title">Total Spending</div>
            <div className="stat-value">
              ₹{summary ? summary.totalAmount.toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00'}
            </div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ backgroundColor: '#dcfce7', color: 'var(--accent)' }}>
            <TrendingUp size={24} />
          </div>
          <div>
            <div className="stat-title">Transactions Recorded</div>
            <div className="stat-value">
              {summary ? summary.transactionCount : 0}
            </div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ backgroundColor: '#fef3c7', color: '#b45309' }}>
            <PieChart size={24} />
          </div>
          <div>
            <div className="stat-title">Active Categories</div>
            <div className="stat-value">
              {summary ? Object.keys(summary.categoryBreakdown).length : 0}
            </div>
          </div>
        </div>
      </div>

      {/* Category Breakdown Progress */}
      {summary && Object.keys(summary.categoryBreakdown).length > 0 && (
        <div className="section-card">
          <h3 className="section-title">Category Spending Breakdown</h3>
          <div>
            {Object.entries(summary.categoryBreakdown).map(([cat, amt]) => {
              const pct = summary.totalAmount > 0 ? (amt / summary.totalAmount) * 100 : 0;
              return (
                <div key={cat} className="category-bar-item">
                  <div className="category-bar-header">
                    <span>{cat}</span>
                    <span style={{ fontWeight: 600 }}>₹{amt.toFixed(2)} ({pct.toFixed(1)}%)</span>
                  </div>
                  <div className="progress-track">
                    <div className="progress-fill" style={{ width: `${Math.min(pct, 100)}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Expenses Table */}
      <div className="section-card">
        <h3 className="section-title">Expense Transactions ({expenses.length})</h3>
        {loading ? (
          <div style={{ padding: 32, textAlign: 'center', color: 'var(--text-muted)' }}>
            Loading records...
          </div>
        ) : expenses.length === 0 ? (
          <div style={{ padding: 32, textAlign: 'center', color: 'var(--text-muted)' }}>
            No expenses found for this date range. Record an expense in the AI Assistant tab!
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="expenses-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Category</th>
                  <th>Description</th>
                  <th style={{ textAlign: 'right' }}>Amount</th>
                  <th style={{ textAlign: 'center', width: 60 }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {expenses.map((exp) => (
                  <tr key={exp.id}>
                    <td>{exp.expenseDate}</td>
                    <td>
                      <span className={`badge ${getBadgeClass(exp.category)}`}>
                        {exp.category}
                      </span>
                    </td>
                    <td>{exp.description || '—'}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600 }}>
                      ₹{exp.amount.toFixed(2)}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <button
                        type="button"
                        onClick={() => handleDelete(exp.id)}
                        disabled={deletingId === exp.id}
                        title="Delete expense"
                        style={{
                          background: 'transparent',
                          border: 'none',
                          cursor: deletingId === exp.id ? 'not-allowed' : 'pointer',
                          color: 'var(--text-muted)',
                          padding: 4,
                          borderRadius: 4,
                          display: 'inline-flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = '#ef4444')}
                        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
                      >
                        <Trash2 size={16} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
