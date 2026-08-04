import React, { useState, useEffect, useRef } from 'react';
import { searchUsers } from '../services/api';
import './Modal.css';

export function NewDirectModal({ onConfirm, onClose }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [selected, setSelected] = useState(null); // { id, username }
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef(null);

  useEffect(() => {
    if (!query.trim() || query.length < 2) {
      setResults([]);
      return;
    }
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await searchUsers(query.trim());
        setResults(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error(e);
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  const handleSelect = (user) => {
    setSelected(user);
    setQuery(user.username);
    setResults([]);
  };

  const handleConfirm = () => {
    if (!selected) return;
    onConfirm(selected.id, selected.username);
  };

  return (
      <Modal title="New Direct Message" onClose={onClose}>
        <div className="modal-field" style={{ position: 'relative' }}>
          <label>Search User</label>
          <input
              value={query}
              onChange={e => { setQuery(e.target.value); setSelected(null); }}
              placeholder="Enter username..."
              autoFocus
          />

          {/* Search results dropdown */}
          {(results.length > 0 || loading) && (
              <div className="search-dropdown">
                {loading && (
                    <div className="search-dropdown-item muted">Searching...</div>
                )}
                {!loading && results.length === 0 && (
                    <div className="search-dropdown-item muted">No results found</div>
                )}
                {results.map(user => (
                    <button
                        key={user.id}
                        className="search-dropdown-item"
                        onClick={() => handleSelect(user)}
                    >
                      <div className="search-avatar">
                        {user.username.slice(0, 2).toUpperCase()}
                      </div>
                      <div className="search-user-info">
                        <span className="search-username">{user.username}</span>
                      </div>
                    </button>
                ))}
              </div>
          )}
        </div>

        {selected && (
            <div className="selected-user-badge">
              <div className="search-avatar small">
                {selected.username.slice(0, 2).toUpperCase()}
              </div>
              <span>{selected.username}</span>
              <button className="remove-selected" onClick={() => { setSelected(null); setQuery(''); }}>✕</button>
            </div>
        )}

        <div className="modal-actions">
          <button className="modal-btn secondary" onClick={onClose}>Cancel</button>
          <button
              className="modal-btn primary"
              disabled={!selected}
              onClick={handleConfirm}
          >
            Start Chat
          </button>
        </div>
      </Modal>
  );
}

export function NewGroupModal({ onConfirm, onClose }) {
  const [name, setName] = useState('');

  return (
      <Modal title="Create New Group" onClose={onClose}>
        <div className="modal-field">
          <label>Group Name</label>
          <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="e.g., project-team"
              autoFocus
          />
        </div>
        <div className="modal-actions">
          <button className="modal-btn secondary" onClick={onClose}>Cancel</button>
          <button
              className="modal-btn primary"
              disabled={!name.trim()}
              onClick={() => onConfirm(name.trim())}
          >
            Create
          </button>
        </div>
      </Modal>
  );
}

export function AddMemberModal({ onConfirm, onClose }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef(null);

  useEffect(() => {
    if (!query.trim() || query.length < 2) { setResults([]); return; }
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await searchUsers(query.trim());
        setResults(Array.isArray(data) ? data : []);
      } catch { setResults([]); }
      finally { setLoading(false); }
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  const handleSelect = (user) => {
    setSelected(user);
    setQuery(user.username);
    setResults([]);
  };

  return (
      <Modal title="Add Member to Group" onClose={onClose}>
        <div className="modal-field" style={{ position: 'relative' }}>
          <label>Search User</label>
          <input
              value={query}
              onChange={e => { setQuery(e.target.value); setSelected(null); }}
              placeholder="Enter username..."
              autoFocus
          />
          {(results.length > 0 || loading) && (
              <div className="search-dropdown">
                {loading && <div className="search-dropdown-item muted">Searching...</div>}
                {results.map(user => (
                    <button key={user.id} className="search-dropdown-item" onClick={() => handleSelect(user)}>
                      <div className="search-avatar">{user.username.slice(0, 2).toUpperCase()}</div>
                      <div className="search-user-info">
                        <span className="search-username">{user.username}</span>
                      </div>
                    </button>
                ))}
              </div>
          )}
        </div>
        {selected && (
            <div className="selected-user-badge">
              <div className="search-avatar small">{selected.username.slice(0, 2).toUpperCase()}</div>
              <span>{selected.username}</span>
              <button className="remove-selected" onClick={() => { setSelected(null); setQuery(''); }}>✕</button>
            </div>
        )}
        <div className="modal-actions">
          <button className="modal-btn secondary" onClick={onClose}>Cancel</button>
          <button
              className="modal-btn primary"
              disabled={!selected}
              onClick={() => onConfirm(selected.id, selected.username)}
          >
            Add
          </button>
        </div>
      </Modal>
  );
}

function Modal({ title, children, onClose }) {
  return (
      <div className="modal-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
        <div className="modal-box">
          <div className="modal-header">
            <h2>{title}</h2>
            <button className="modal-close" onClick={onClose}>✕</button>
          </div>
          {children}
        </div>
      </div>
  );
}
