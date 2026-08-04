import React, {memo, useEffect, useLayoutEffect, useRef, useState} from 'react';
import {
    AddMemberIcon, BlockIcon, BubbleIcon, DoubleTickIcon,
    MembersIcon, ReplyIcon, SendIcon, SingleTickIcon, TrashIcon
} from './Icons';
import './ChatWindow.css';

function formatTime(dateStr) {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

function formatLastSeen(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const today = new Date();
    const time = date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

    const dateMidnight = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const diffDays = Math.round((todayMidnight - dateMidnight) / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return `Today at ${time}`;
    if (diffDays === 1) return `Yesterday at ${time}`;
    if (diffDays >= 2 && diffDays <= 6) {
        const weekday = date.toLocaleDateString('en-US', { weekday: 'long' });
        return `${weekday} at ${time}`;
    }
    return `${date.toLocaleDateString('en-US')} ${time}`;
}

function formatDateLabel(dateStr) {
    if (!dateStr) return '';
    const msgDate = new Date(dateStr);
    const today = new Date();

    const msgMidnight = new Date(msgDate.getFullYear(), msgDate.getMonth(), msgDate.getDate());
    const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate());

    const diffDays = Math.round((todayMidnight - msgMidnight) / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays >= 2 && diffDays <= 6) {
        return msgDate.toLocaleDateString('en-US', { weekday: 'long' });
    }
    return msgDate.toLocaleDateString('en-US', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function groupMessagesByDate(messages) {
    const groups = [];
    messages.forEach(msg => {
        const label = formatDateLabel(msg.sentAt);
        const last = groups[groups.length - 1];
        if (!last || last.label !== label) {
            groups.push({ label, messages: [msg] });
        } else {
            last.messages.push(msg);
        }
    });
    return groups;
}

function Avatar({ name, size = 32 }) {
    const initials = name ? name.slice(0, 2).toUpperCase() : '?';
    const colors = ['#2b5278', '#3a7cbd', '#2e7d52', '#7c4d8a', '#8a5d2e'];
    const color = colors[name?.charCodeAt(0) % colors.length] || colors[0];
    return (
        <div className="msg-avatar" style={{ width: size, height: size, background: color }}>
            {initials}
        </div>
    );
}

const MessageRow = memo(({ msg, isMe, showAvatar, showName, repliedMsg, onReply, onDelete }) => {

    const renderTicks = () => {
        if (!isMe) return null;
        if (!msg.id) return <span className="msg-tick" style={{ color: '#8696a0', marginLeft: '4px' }}><SingleTickIcon /></span>;
        if (!msg.delivered && !msg.read) return <span className="msg-tick" style={{ color: '#8696a0', marginLeft: '4px' }}><SingleTickIcon /></span>;
        if (msg.delivered && !msg.read) return <span className="msg-tick" style={{ color: '#8696a0', marginLeft: '4px' }}><DoubleTickIcon /></span>;
        return <span className="msg-tick read" style={{ color: '#34b7f1', marginLeft: '4px' }}><DoubleTickIcon /></span>;
    };

    if (msg.type === 'SYSTEM') {
        return (
            <div id={msg.id ? `msg-${msg.id}` : ''} className="date-divider" style={{ margin: '8px 0' }}>
                <span className="date-divider-label" style={{ background: '#182229', color: '#8696a0', fontSize: '12px', fontStyle: 'italic', padding: '5px 12px' }}>
                    {msg.content}
                </span>
            </div>
        );
    }

    return (
        <div id={msg.id ? `msg-${msg.id}` : ''} className={`msg-row ${isMe ? 'me' : 'them'} ${msg.deleted ? 'deleted' : ''}`}>
            {!isMe && (
                <div className="msg-avatar-col">
                    {showAvatar ? <Avatar name={msg.senderUsername} /> : <div style={{ width: 32 }} />}
                </div>
            )}
            <div className="msg-col">
                {showName && (
                    <div className="msg-sender-name">{msg.senderUsername}</div>
                )}
                <div className="msg-bubble-wrapper">
                    <div className={`msg-bubble ${isMe ? 'me' : 'them'}`}>
                        {repliedMsg && !msg.deleted && (
                            <div
                                className="msg-reply-preview"
                                onClick={() => document.getElementById(`msg-${repliedMsg.id}`)?.scrollIntoView({ behavior: 'smooth' })}
                            >
                                <strong>{repliedMsg.senderUsername}</strong>
                                <p>{repliedMsg.content}</p>
                            </div>
                        )}
                        <span className="msg-text">
                            {msg.deleted ? (<> <BlockIcon /> {isMe ? 'You deleted this message' : 'This message was deleted'} </>) : ( msg.content)}
                        </span>
                        <div className="msg-meta" style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '4px', marginTop: '2px', alignSelf: 'flex-end' }}>
                            <span className="msg-time" style={{ fontSize: '11px', color: '#8696a0' }}>
                                {formatTime(msg.sentAt)}
                            </span>
                            {!msg.deleted && renderTicks()}
                        </div>
                    </div>
                    {!msg.deleted && (
                        <div className="msg-actions">
                            <button title="Reply" onClick={() => onReply(msg)}>
                                <ReplyIcon />
                            </button>
                            {isMe && (
                                <button title="Delete for everyone" onClick={() => onDelete(msg.id)}>
                                    <TrashIcon />
                                </button>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
});

export default function ChatWindow({
                                       conversation,
                                       messages,
                                       currentUser,
                                       onSend,
                                       onAddMember,
                                       typingUsers,
                                       onTyping,
                                       onDeleteMessage,
                                       hasMore,
                                       onLoadMore,
                                       userStatuses = {}
                                   }) {
    const [text, setText] = useState('');
    const [showMembers, setShowMembers] = useState(false);
    const [replyingTo, setReplyingTo] = useState(null);
    const listRef = useRef(null);
    const bottomRef = useRef(null);
    const prevMessagesRef = useRef(messages);
    const prevScrollHeightRef = useRef(0);
    const typingThrottle = useRef(false);

    useLayoutEffect(() => {
        const container = listRef.current;
        if (!container) return;

        if (messages.length > prevMessagesRef.current.length && prevMessagesRef.current.length > 0 && messages[0]?.id !== prevMessagesRef.current[0]?.id) {
            container.scrollTop = container.scrollHeight - prevScrollHeightRef.current;
        }
        prevMessagesRef.current = messages;
    }, [messages]);

    useEffect(() => {
        const container = listRef.current;
        if (!container) return;

        const isNearBottom = container.scrollHeight - container.scrollTop - container.clientHeight < 300;
        const lastMsg = messages.length > 0 ? messages[messages.length - 1] : null;
        const isMe = lastMsg ? Number(lastMsg.senderId) === Number(currentUser.userId) : false;
        const isTyping = typingUsers && typingUsers.length > 0;

        if (isMe || isNearBottom || isTyping || messages.length <= 50) {
            const behavior = messages.length <= 50 && !isMe && !isTyping ? 'auto' : 'smooth';
            bottomRef.current?.scrollIntoView({ behavior });
        }
    }, [messages.length, typingUsers, currentUser.userId]);

    const handleScroll = (e) => {
        const container = e.currentTarget;
        if (container.scrollTop <= 5 && hasMore && onLoadMore) {
            prevScrollHeightRef.current = container.scrollHeight;
            onLoadMore();
        }
    };

    const handleSend = () => {
        const trimmed = text.trim();
        if (!trimmed) return;
        onSend(trimmed, replyingTo?.id);
        setText('');
        setReplyingTo(null);
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleTextChange = (e) => {
        setText(e.target.value);
        if (e.target.value.trim() && !typingThrottle.current && onTyping) {
            onTyping();
            typingThrottle.current = true;
            setTimeout(() => { typingThrottle.current = false; }, 2000);
        }
    };

    if (!conversation) {
        return (
            <div className="chat-empty">
                <div className="chat-empty-inner">
                    <BubbleIcon />
                    <h2>Start messaging</h2>
                    <p>Select a chat from the left menu or start a new conversation</p>
                </div>
            </div>
        );
    }

    const isGroup = conversation.type === 'GROUP';
    const myId = Number(currentUser.userId);
    const groups = groupMessagesByDate(messages);

    const isSelf = !isGroup && Number(conversation.id) === myId;
    const displayName = isSelf ? `${conversation.name} (You)` : conversation.name;

    return (
        <div className="chat-window">
            <div className="chat-header">
                <div className="chat-header-info">
                    <div className="chat-header-name">{displayName}</div>
                    <div className="chat-header-sub">
                        {isGroup
                            ? `${conversation.members?.length ?? 0} members`
                            : (
                                isSelf ? 'Saved Messages' :
                                    userStatuses[conversation.id]?.isOnline
                                        ? <span style={{ color: '#00a884', fontWeight: '500' }}>Online</span>
                                        : (userStatuses[conversation.id]?.lastSeen
                                                ? `Last seen ${formatLastSeen(userStatuses[conversation.id].lastSeen)}`
                                                : 'Offline'
                                        )
                            )
                        }
                    </div>
                </div>
                {isGroup && (
                    <button className="icon-btn-chat" title="Add member" onClick={onAddMember}>
                        <AddMemberIcon />
                    </button>
                )}
                {isGroup && (
                    <button
                        className={`icon-btn-chat ${showMembers ? 'active' : ''}`}
                        title="Members"
                        onClick={() => setShowMembers(v => !v)}
                    >
                        <MembersIcon />
                    </button>
                )}
            </div>

            {showMembers && isGroup && (
                <div className="members-panel">
                    <div className="members-panel-title">Members</div>
                    {(conversation.members || []).map(m => (
                        <div key={m.userId} className="member-row">
                            <Avatar name={m.username} size={28} />
                            <span>{m.username}</span>
                            {m.userId === conversation.createdBy && (
                                <span className="owner-badge">Owner</span>
                            )}
                        </div>
                    ))}
                </div>
            )}

            <div className="chat-messages" ref={listRef} onScroll={handleScroll}>
                {hasMore && (
                    <div className="date-divider" style={{ margin: '5px 0' }}>
                        <span className="date-divider-label" style={{ background: '#182229', color: '#8696a0', fontSize: '11px' }}>
                            Loading older messages...
                        </span>
                    </div>
                )}
                {messages.length === 0 && !hasMore && (
                    <div className="messages-empty">
                        No messages yet. Send the first message!
                    </div>
                )}

                {groups.map((group, gi) => (
                    <React.Fragment key={group.label + gi}>
                        <div className="date-divider">
                            <span className="date-divider-label">{group.label}</span>
                        </div>
                        {group.messages.map((msg, i) => {
                            const isMe = Number(msg.senderId) === myId;
                            const prevMsg = i === 0 ? group.messages[group.messages.length - 1] : group.messages[i - 1];
                            const showAvatar = !isMe && (i === 0 || prevMsg?.senderId !== msg.senderId);
                            const showName = isGroup && !isMe && showAvatar;
                            const repliedMsg = msg.replyToId ? messages.find(m => m.id === msg.replyToId) : null;

                            return (
                                <MessageRow
                                    key={msg.id ?? `${gi}-${i}`}
                                    msg={msg}
                                    isMe={isMe}
                                    showAvatar={showAvatar}
                                    showName={showName}
                                    repliedMsg={repliedMsg}
                                    onReply={setReplyingTo}
                                    onDelete={onDeleteMessage}
                                />
                            );
                        })}
                    </React.Fragment>
                ))}

                {typingUsers && typingUsers.length > 0 && (
                    <div className="typing-indicator-container">
                        <span className="typing-username">
                            {typingUsers.length === 1 ? typingUsers[0] : typingUsers.join(', ')}
                        </span>
                        &nbsp;{typingUsers.length === 1 ? 'is typing' : 'are typing'}
                        <span className="typing-dots"><span>.</span><span>.</span><span>.</span></span>
                    </div>
                )}
                <div ref={bottomRef} />
            </div>

            <div className="chat-input-wrapper">
                {replyingTo && (
                    <div className="replying-to-bar">
                        <div>
                            <strong>Replying to {replyingTo.senderUsername}</strong>
                            <p>{replyingTo.content}</p>
                        </div>
                        <button onClick={() => setReplyingTo(null)}>×</button>
                    </div>
                )}
                <div className="chat-input-bar">
                    <textarea
                        className="chat-input"
                        value={text}
                        onChange={handleTextChange}
                        onKeyDown={handleKeyDown}
                        placeholder="Type a message..."
                        rows={1}
                    />
                    <button
                        className={`send-btn ${text.trim() ? 'active' : ''}`}
                        onClick={handleSend}
                        disabled={!text.trim()}
                        title="Send"
                    >
                        <SendIcon />
                    </button>
                </div>
            </div>
        </div>
    );
}
