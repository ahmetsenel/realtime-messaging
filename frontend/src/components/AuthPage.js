import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { login as apiLogin, register as apiRegister } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './AuthPage.css';

export default function AuthPage() {
  const { login } = useAuth();
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [animate, setAnimate] = useState(false);

  useEffect(() => {
    setAnimate(true);
    const timer = setTimeout(() => setAnimate(false), 300);
    return () => clearTimeout(timer);
  }, [mode]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (mode === 'login') {
        const data = await apiLogin(username, password);
        login(data);
      } else {
        await apiRegister(username, password);
        toast.success('Registration successful! Please log in.');
        setMode('login');
        setPassword('');
      }
    } catch (err) {
    } finally {
      setLoading(false);
    }
  };

  return (
      <div className="auth-bg">
        <div className={`auth-card ${animate ? 'fade-in' : ''}`}>
          <div className="auth-logo">
            <svg width="48" height="48" viewBox="0 0 40 40" fill="none">
              <circle cx="20" cy="20" r="20" fill="url(#logo-gradient)"/>
              <path d="M10 27L14 19L20 23L26 14L30 27H10Z" fill="#ffffff" opacity="0.4"/>
              <circle cx="20" cy="17" r="5" fill="#ffffff"/>
              <path d="M14 28L17 22H23L26 28" fill="#ffffff"/>
              <defs>
                <linearGradient id="logo-gradient" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
                  <stop stopColor="var(--accent)" />
                  <stop offset="1" stopColor="#2b5278" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 className="auth-title">Messaging</h1>
          <p className="auth-subtitle">
            {mode === 'login' ? 'Log in to your account' : 'Create a new account'}
          </p>

          <form onSubmit={handleSubmit} className="auth-form">
            <div className="auth-field">
              <label>Username</label>
              <div className="input-wrapper">
                <svg className="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <input
                    type="text"
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    placeholder="Enter username"
                    autoFocus
                    required
                />
              </div>
            </div>

            <div className="auth-field">
              <label>Password</label>
              <div className="input-wrapper">
                <svg className="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
                <input
                    type="password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    placeholder="Set a password"
                    required
                />
              </div>
            </div>

            <button type="submit" className="auth-btn" disabled={loading}>
              {loading ? <span className="loader"></span> : mode === 'login' ? 'Log In' : 'Sign Up'}
            </button>
          </form>

          <p className="auth-switch">
            {mode === 'login' ? "Don't have an account?" : 'Already have an account?'}
            <button
                type="button"
                className="auth-switch-btn"
                onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
            >
              {mode === 'login' ? 'Sign Up' : 'Log In'}
            </button>
          </p>
        </div>
      </div>
  );
}