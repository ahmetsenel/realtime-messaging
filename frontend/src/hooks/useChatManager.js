import { useState, useEffect, useCallback, useRef } from 'react';
import {
    getMyDirectMessages, getGroupMessages, getDirectMessages,
    getMyGroups, createGroup, addGroupMember, getActiveUsers, getUserStatuses
} from '../services/api';
import {
    connectChat, subscribeGroup, sendDirectMessage, sendGroupMessage,
    sendReadReceipt, sendDeliverySignal, disconnectAll
} from '../services/websocket';

function playNotificationSound() {
    try {
        const audio = new Audio('/sounds/notification.mp3');
        audio.play().catch(err => console.log('Audio could not be played:', err));
    } catch (e) {
        console.error('Error while playing audio:', e);
    }
}

function getPeer(msg, myId) {
    const me = Number(myId);
    if (Number(msg.senderId) === me) {
        return { id: Number(msg.receiverId), name: msg.receiverUsername };
    }
    return { id: Number(msg.senderId), name: msg.senderUsername };
}

function formatSystemMessage(content, currentUsername) {
    if (!content) return content;
    if (content === `${currentUsername} was added to the group`) return "You were added to the group";
    if (content === `${currentUsername} created the group`) return "You created the group";
    return content;
}

export function useChatManager(auth) {
    const myId = Number(auth?.userId);
    const [conversations, setConversations] = useState({});
    const [messages, setMessages] = useState({});
    const [activeKey, setActiveKey] = useState(null);
    const [wsReady, setWsReady] = useState(false);
    const [typingIndicators, setTypingIndicators] = useState({});
    const [userStatuses, setUserStatuses] = useState({});
    const [chatPages, setChatPages] = useState({});

    const activeKeyRef = useRef(null);
    const conversationsRef = useRef(conversations);
    const subscribedGroups = useRef(new Set());
    const typingTimeouts = useRef({});
    const hasSentDelivery = useRef(false);

    useEffect(() => {
        conversationsRef.current = conversations;
    }, [conversations]);

    useEffect(() => {
        activeKeyRef.current = activeKey;
    }, [activeKey]);

    useEffect(() => {
        const convs = Object.values(conversations);
        if (wsReady && convs.length > 0 && !hasSentDelivery.current) {
            hasSentDelivery.current = true;
            setTimeout(() => {
                convs.forEach(conv => sendDeliverySignal(conv.type, conv.id));
            }, 1000);
        }
    }, [wsReady, conversations]);

    const handleUserStatusChange = useCallback((data) => {
        const targetUserId = Number(data.userId);
        setUserStatuses(prev => ({
            ...prev,
            [targetUserId]: { isOnline: data.status === 'ONLINE', lastSeen: data.lastSeen || null }
        }));
    }, []);

    const handleTypingEvent = useCallback((typingEvent) => {
        if (typingEvent.typerId === myId) return;
        const key = typingEvent.type === 'DIRECT' ? `DIRECT_${typingEvent.typerId}` : `GROUP_${typingEvent.groupId}`;

        setTypingIndicators(prev => {
            const users = prev[key] || [];
            if (!users.includes(typingEvent.typerName)) return { ...prev, [key]: [...users, typingEvent.typerName] };
            return prev;
        });

        const timerKey = `${key}_${typingEvent.typerId}`;
        if (typingTimeouts.current[timerKey]) clearTimeout(typingTimeouts.current[timerKey]);

        typingTimeouts.current[timerKey] = setTimeout(() => {
            setTypingIndicators(prev => {
                const users = prev[key] || [];
                return { ...prev, [key]: users.filter(name => name !== typingEvent.typerName) };
            });
        }, 2500);
    }, [myId]);

    const handleDeleteEvent = useCallback((deleteEvent) => {
        const key = deleteEvent.type === 'DIRECT' ? `DIRECT_${deleteEvent.peerId}` : `GROUP_${deleteEvent.groupId}`;

        setMessages(prevMsgs => {
            if (!prevMsgs[key]) return prevMsgs;

            const targetMsg = prevMsgs[key].find(m => m.id === deleteEvent.messageId);
            const isMyMsg = targetMsg && Number(targetMsg.senderId) === myId;
            const deletedText = isMyMsg ? 'You deleted this message' : 'This message was deleted';
            const isLastMessage = prevMsgs[key][prevMsgs[key].length - 1]?.id === deleteEvent.messageId;

            if (isLastMessage) {
                setConversations(prevConvs => {
                    if (!prevConvs[key]) return prevConvs;
                    return {
                        ...prevConvs,
                        [key]: { ...prevConvs[key], lastMessage: deletedText, isLastDeleted: true }
                    };
                });
            }

            return {
                ...prevMsgs,
                [key]: prevMsgs[key].map(m => m.id === deleteEvent.messageId ? { ...m, deleted: true, content: deletedText } : m)
            };
        });
    }, [myId]);

    const addMessage = useCallback((key, msg) => {
        if (activeKeyRef.current === key && msg.senderId !== myId) {
            msg.read = true;
            sendReadReceipt(msg.type, msg.type === 'DIRECT' ? msg.senderId : msg.groupId);
        }
        setMessages(prev => {
            const existing = prev[key] ?? [];
            if (msg.id && existing.some(m => m.id === msg.id)) return prev;
            return { ...prev, [key]: [...existing, msg] };
        });
        setConversations(prev => {
            const conv = prev[key];
            if (!conv) return prev;
            let newUnread = conv.unread ?? 0;
            if (key !== activeKeyRef.current && msg.senderId !== myId && msg.type !== 'SYSTEM') newUnread += 1;
            else if (key === activeKeyRef.current) newUnread = 0;

            return {
                ...prev,
                [key]: {
                    ...conv,
                    lastMessage: msg.content,
                    lastTime: msg.sentAt,
                    lastSender: msg.type === 'SYSTEM' ? null : (msg.senderUsername ?? null),
                    unread: newUnread,
                    isLastDeleted: false
                },
            };
        });
        setTypingIndicators(prev => {
            const users = prev[key] || [];
            if (msg.senderUsername && users.includes(msg.senderUsername)) {
                return { ...prev, [key]: users.filter(name => name !== msg.senderUsername) };
            }
            return prev;
        });
    }, [myId]);

    const loadMessages = useCallback(async (conv, page = 0) => {
        try {
            const history = conv.type === 'DIRECT' ? await getDirectMessages(conv.id, page, 50) : await getGroupMessages(conv.id, page, 50);
            const fetchedMessages = Array.isArray(history) ? history : [];
            setChatPages(prev => ({ ...prev, [conv.key]: { page, hasMore: fetchedMessages.length === 50 } }));

            setMessages(prev => {
                const existing = prev[conv.key] || [];
                let combined = page === 0 ? [...fetchedMessages] : [...fetchedMessages, ...existing];
                combined = combined.map(m => m.type === 'SYSTEM' ? { ...m, content: formatSystemMessage(m.content, auth.username) } : m);

                const unique = [];
                const seen = new Set();
                combined.forEach(m => {
                    if (m.id && !seen.has(m.id)) { seen.add(m.id); unique.push(m); }
                    else if (!m.id) unique.push(m);
                });
                unique.sort((a, b) => new Date(a.sentAt) - new Date(b.sentAt));
                return { ...prev, [conv.key]: unique.map(m => (m.senderId !== myId && !m.read) ? { ...m, read: true } : m) };
            });
        } catch (e) {
            console.error(e);
        }
    }, [myId, auth?.username]);

    const subscribeToGroup = useCallback((groupId) => {
        if (subscribedGroups.current.has(groupId)) return;
        subscribedGroups.current.add(groupId);
        const key = `GROUP_${groupId}`;

        subscribeGroup(
            groupId,
            (msg) => {
                if (msg.type === 'SYSTEM') msg.content = formatSystemMessage(msg.content, auth.username);
                if (msg.senderId !== myId && msg.type !== 'SYSTEM') playNotificationSound();
                addMessage(key, msg);
            },
            (receipt) => {
                if (Number(receipt.readerId) !== myId) {
                    const group = conversationsRef.current[key];
                    const requiredReads = group && group.members ? group.members.length - 1 : 1;
                    setMessages(prev => {
                        if (!prev[key]) return prev;
                        return {
                            ...prev,
                            [key]: prev[key].map(m => {
                                if (Number(m.senderId) === Number(myId) && !m.read) {
                                    const currentReadBy = new Set(m.readByUsers || []);
                                    currentReadBy.add(receipt.readerId);
                                    return { ...m, readByUsers: Array.from(currentReadBy), read: currentReadBy.size >= requiredReads };
                                }
                                return m;
                            })
                        };
                    });
                }
            },
            handleTypingEvent,
            (receipt) => {
                if (Number(receipt.delivererId) !== myId) {
                    const group = conversationsRef.current[key];
                    const requiredReads = group && group.members ? group.members.length - 1 : 1;
                    setMessages(prev => {
                        if (!prev[key]) return prev;
                        return {
                            ...prev,
                            [key]: prev[key].map(m => {
                                if (Number(m.senderId) === Number(myId) && !m.delivered) {
                                    const currentDeliveredTo = new Set(m.deliveredToUsers || []);
                                    currentDeliveredTo.add(receipt.delivererId);
                                    return { ...m, deliveredToUsers: Array.from(currentDeliveredTo), delivered: currentDeliveredTo.size >= requiredReads };
                                }
                                return m;
                            })
                        };
                    });
                }
            },
            handleDeleteEvent
        );
    }, [addMessage, myId, auth?.username, handleTypingEvent, handleDeleteEvent]);

    useEffect(() => {
        if (!auth?.token) return;
        let cancelled = false;

        Promise.all([
            getMyDirectMessages().catch(() => []),
            getMyGroups().catch(() => []),
            getActiveUsers().catch(() => []),
            getUserStatuses().catch(() => ({}))
        ]).then(([msgs, groups, activeUserIds, dbStatuses]) => {
            if (cancelled) return;
            const nextConvs = {};

            if (Array.isArray(msgs)) msgs.forEach(summary => nextConvs[summary.key] = summary);
            if (Array.isArray(groups)) {
                groups.forEach(group => {
                    const key = `GROUP_${group.id}`;
                    nextConvs[key] = {
                        key, type: 'GROUP', id: group.id, name: group.name, members: group.members,
                        createdBy: group.createdBy, lastMessage: formatSystemMessage(group.lastMessage || 'No messages', auth.username),
                        lastTime: group.lastMessageTime || group.createdAt, lastSender: group.lastSender || null, unread: group.unreadCount || 0,
                    };
                });
            }
            setConversations(nextConvs);

            const initialStatuses = {};
            if (dbStatuses && typeof dbStatuses === 'object') {
                Object.keys(dbStatuses).forEach(id => initialStatuses[Number(id)] = { isOnline: false, lastSeen: dbStatuses[id] });
            }
            if (Array.isArray(activeUserIds)) {
                activeUserIds.forEach(id => initialStatuses[Number(id)] = { isOnline: true, lastSeen: null });
            }
            setUserStatuses(initialStatuses);

            connectChat(auth.token, {
                onConnect: () => {
                    setWsReady(true);
                    subscribedGroups.current.clear();
                    if (Array.isArray(groups)) groups.forEach(group => subscribeToGroup(group.id));
                },
                onDisconnect: () => setWsReady(false),
                onGroupAdded: (groupData) => {
                    const key = `GROUP_${groupData.id}`;
                    setConversations(prev => prev[key] ? prev : {
                        ...prev,
                        [key]: {
                            key, type: 'GROUP', id: groupData.id, name: groupData.name, members: groupData.members || [],
                            createdBy: groupData.createdBy, lastMessage: 'You were added to the group', lastTime: new Date().toISOString(),
                            lastSender: null, unread: 0,
                        }
                    });
                    setMessages(prev => ({ ...prev, [key]: [] }));
                    subscribeToGroup(groupData.id);
                },
                onMessage: (msg) => {
                    if (msg.type !== 'DIRECT') return;
                    if (msg.senderId !== myId) playNotificationSound();
                    const peer = getPeer(msg, myId);
                    const key = `DIRECT_${peer.id}`;
                    addMessage(key, msg);
                    setConversations(prev => prev[key] ? prev : {
                        ...prev,
                        [key]: {
                            key, type: 'DIRECT', id: peer.id, name: peer.name,
                            lastMessage: msg.content, lastTime: msg.sentAt,
                            lastSender: msg.senderUsername ?? null, unread: Number(msg.senderId) !== myId ? 1 : 0,
                        },
                    });
                },
                onReadReceipt: (receipt) => {
                    if (receipt.type === 'DIRECT') {
                        const key = `DIRECT_${receipt.readerId}`;
                        setMessages(prev => !prev[key] ? prev : { ...prev, [key]: prev[key].map(m => (Number(m.senderId) === Number(myId) && !m.read) ? { ...m, read: true } : m) });
                    }
                },
                onDeliver: (receipt) => {
                    if (receipt.type === 'DIRECT') {
                        const key = `DIRECT_${receipt.delivererId}`;
                        setMessages(prev => !prev[key] ? prev : {
                            ...prev,
                            [key]: prev[key].map(m =>
                                (Number(m.senderId) === Number(myId) && !m.delivered) ? { ...m, delivered: true } : m
                            )
                        });
                    }
                },
                onTyping: handleTypingEvent,
                onUserStatusChange: handleUserStatusChange,
                onDelete: handleDeleteEvent
            });
        }).catch(console.error);

        return () => {
            cancelled = true;
            disconnectAll();
        };
    }, [auth?.token, auth?.userId, myId, addMessage, subscribeToGroup, handleTypingEvent, handleUserStatusChange, handleDeleteEvent]);

    const handleSelect = async (conv) => {
        setActiveKey(conv.key);
        sendReadReceipt(conv.type, conv.id);
        setConversations(prev => ({ ...prev, [conv.key]: { ...prev[conv.key], unread: 0 } }));

        if (!chatPages[conv.key]) await loadMessages(conv, 0);
        else {
            setMessages(prev => ({
                ...prev,
                [conv.key]: (prev[conv.key] || []).map(m => (m.senderId !== myId && !m.read) ? { ...m, read: true } : m)
            }));
        }
        if (conv.type === 'GROUP' && wsReady) subscribeToGroup(conv.id);
    };

    const handleSend = (content, replyToId = null) => {
        if (!activeKey) return;
        const conv = conversations[activeKey];
        conv.type === 'DIRECT' ? sendDirectMessage(conv.id, conv.name, content, replyToId) : sendGroupMessage(conv.id, content, replyToId);
    };

    const handleLoadMore = useCallback(async () => {
        if (!activeKeyRef.current) return;
        const conv = conversationsRef.current[activeKeyRef.current];
        const pageData = chatPages[activeKeyRef.current] || { page: 0, hasMore: false };
        if (conv && pageData.hasMore) await loadMessages(conv, pageData.page + 1);
    }, [chatPages, loadMessages]);

    const handleNewDirect = (userId, username) => {
        const id = Number(userId);
        const key = `DIRECT_${id}`;

        if (conversations[key]) {
            handleSelect(conversations[key]).catch(console.error);
            return;
        }

        setConversations(prev => ({
            ...prev,
            [key]: { key, type: 'DIRECT', id, name: username, lastMessage: '', lastTime: null, unread: 0 }
        }));
        setMessages(prev => ({ ...prev, [key]: [] }));
        setActiveKey(key);
    };

    const handleNewGroup = async (name) => {
        try {
            const group = await createGroup(name);
            const key = `GROUP_${group.id}`;
            let displayMsg = group.lastMessage || 'You created the group';
            if (displayMsg === `${auth.username} created the group`) displayMsg = 'You created the group';

            setConversations(prev => ({ ...prev, [key]: { key, type: 'GROUP', id: group.id, name: group.name, members: group.members, createdBy: group.createdBy, lastMessage: displayMsg, lastTime: group.lastMessageTime || group.createdAt, lastSender: null, unread: 0 } }));
            setMessages(prev => ({ ...prev, [key]: [] }));
            setActiveKey(key);
            subscribeToGroup(group.id);
        } catch (e) { console.error(e); }
    };

    const handleAddMember = async (userId, username) => {
        const conv = conversations[activeKey];
        if (!conv || conv.type !== 'GROUP') return;
        try {
            await addGroupMember(conv.id, userId, username);
            setConversations(prev => ({ ...prev, [activeKey]: { ...prev[activeKey], members: [...(prev[activeKey].members ?? []), { userId, username, joinedAt: new Date().toISOString() }] } }));
        } catch (e) { console.error(e); }
    };

    return {
        conversations, messages, activeKey, wsReady, typingIndicators, userStatuses, chatPages,
        handleSelect, handleSend, handleLoadMore, handleNewDirect, handleNewGroup, handleAddMember
    };
}