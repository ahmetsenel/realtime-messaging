import React, { useState, memo } from 'react';
import { SearchIcon, PencilIcon, GroupIcon, LogoutIcon, BlockIcon } from './Icons';
import './Sidebar.css';

function formatTime(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const today = new Date();
    const dateMidnight = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const diffDays = Math.round((todayMidnight - dateMidnight) / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
        return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
        return 'Yesterday';
    } else if (diffDays >= 2 && diffDays <= 6) {
        return date.toLocaleDateString('en-US', { weekday: 'long' });
    } else {
        return date.toLocaleDateString('en-US', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
}

function Avatar({ name, size = 42 }) {
    const initials = name ? name.slice(0, 2).toUpperCase() : '?';
    const colors = ['#2b5278', '#3a7cbd', '#2e7d52', '#7c4d8a', '#8a5d2e'];
    const color = colors[name?.charCodeAt(0) % colors.length] || colors[0];
    return (
        <div className="avatar" style={{ width: size, height: size, background: color }}>
            {initials}
        </div>
    );
}

const ConversationItem = memo(({ conv, isActive, isTyping, isOnline, currentUser, onSelect }) => {
    const isDeleted = conv.isLastDeleted === true;
    const displayMsg = conv.lastMessage;
    const isSelf = conv.type === 'DIRECT' && Number(conv.id) === Number(currentUser?.userId);
    const displayName = isSelf ? `${conv.name} (You)` : conv.name;

    return (
        <button
            className={`conv-item ${isActive ? 'active' : ''}`}
            onClick={() => onSelect(conv).catch(console.error)}
        >
            <div className="conv-avatar-wrap">
                <Avatar name={conv.name} />
                {isOnline && !isSelf && <span className="online-dot" />}
            </div>
            <div className="conv-info">
                <div className="conv-row">
                    <span className="conv-name">{displayName}</span>
                    <span className="conv-time">{formatTime(conv.lastTime)}</span>
                </div>
                <div className="conv-row">
                    <span className="conv-last">
                        {isTyping ? (
                            <span className="sidebar-typing">
                                {conv.type === 'DIRECT'
                                    ? 'is typing...'
                                    : `${isTyping} is typing...`}
                            </span>
                        ) : (
                            (() => {
                                if (conv.type === 'GROUP') {
                                    return displayMsg ? (
                                        <>
                                            <span className="conv-last-sender">
                                                {conv.lastSender && conv.lastSender !== currentUser?.username ? `${conv.lastSender}: ` : ''}
                                            </span>
                                            {isDeleted && <BlockIcon />}
                                            <span style={isDeleted ? { fontStyle: 'italic', opacity: 0.7 } : {}}>{displayMsg}</span>
                                        </>
                                    ) : <span className="conv-last-empty">No messages</span>;
                                } else {
                                    return displayMsg ? (
                                        <>
                                            {isDeleted && <BlockIcon />}
                                            <span style={isDeleted ? { fontStyle: 'italic', opacity: 0.7 } : {}}>{displayMsg}</span>
                                        </>
                                    ) : <span className="conv-last-empty">No messages</span>;
                                }
                            })()
                        )}
                    </span>
                    {conv.unread > 0 && !isTyping && (
                        <span className="conv-badge">{conv.unread}</span>
                    )}
                </div>
            </div>
        </button>
    );
});

export default function Sidebar({
                                    currentUser,
                                    conversations,
                                    activeId,
                                    onSelect,
                                    onNewDirect,
                                    onNewGroup,
                                    onLogout,
                                    typingIndicators = {},
                                    userStatuses = {}
                                }) {
    const [search, setSearch] = useState('');
    const filtered = conversations.filter(c =>
        c.name.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <aside className="sidebar">
            <div className="sidebar-header">
                <div className="sidebar-user">
                    <Avatar name={currentUser?.username} size={36} />
                    <span className="sidebar-username">{currentUser?.username}</span>
                </div>
                <div className="sidebar-actions">
                    <button className="icon-btn" title="New direct message" onClick={onNewDirect}>
                        <PencilIcon />
                    </button>
                    <button className="icon-btn" title="New group" onClick={onNewGroup}>
                        <GroupIcon />
                    </button>
                    <button className="icon-btn" title="Log out" onClick={onLogout}>
                        <LogoutIcon />
                    </button>
                </div>
            </div>

            <div className="sidebar-search">
                <SearchIcon />
                <input
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder="Search chats..."
                />
            </div>

            <div className="conv-list">
                {filtered.length === 0 && (
                    <div className="conv-empty">
                        No chats yet.<br />
                        <span>Click the ✎ icon to start a new message</span>
                    </div>
                )}
                {filtered.map(conv => {
                    const isTypingName = typingIndicators[conv.key] && typingIndicators[conv.key].length > 0 ? typingIndicators[conv.key][0] : null;
                    const isUserOnline = conv.type === 'DIRECT' && userStatuses[Number(conv.id)]?.isOnline;

                    return (
                        <ConversationItem
                            key={conv.key}
                            conv={conv}
                            isActive={activeId === conv.key}
                            isTyping={isTypingName}
                            isOnline={isUserOnline}
                            currentUser={currentUser}
                            onSelect={onSelect}
                        />
                    );
                })}
            </div>
        </aside>
    );
}