import React, { useState } from 'react';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import AuthPage from './components/AuthPage';
import Sidebar from './components/Sidebar';
import ChatWindow from './components/ChatWindow';
import { NewDirectModal, NewGroupModal, AddMemberModal } from './components/Modal';
import { useChatManager } from './hooks/useChatManager';
import { disconnectAll, sendTypingSignal, sendDeleteSignal } from './services/websocket';
import './App.css';

function MessagingApp() {
    const { auth, logout } = useAuth();
    const [modal, setModal] = useState(null);

    const {
        conversations, messages, activeKey, wsReady,
        typingIndicators, userStatuses, chatPages,
        handleSelect, handleSend, handleLoadMore,
        handleNewDirect, handleNewGroup, handleAddMember
    } = useChatManager(auth, logout);

    const sortedConvs = Object.values(conversations).sort((a, b) => {
        if (!a.lastTime) return 1;
        if (!b.lastTime) return -1;
        return new Date(b.lastTime) - new Date(a.lastTime);
    });

    const activeConv = activeKey ? conversations[activeKey] : null;
    const activeMessages = activeKey ? (messages[activeKey] ?? []) : [];
    const activeHasMore = activeKey ? (chatPages[activeKey]?.hasMore ?? false) : false;

    return (
        <div className="app-layout">
            <Sidebar
                currentUser={auth}
                conversations={sortedConvs}
                activeId={activeKey}
                onSelect={handleSelect}
                onNewDirect={() => setModal('direct')}
                onNewGroup={() => setModal('group')}
                onLogout={() => { disconnectAll(); logout(); }}
                typingIndicators={typingIndicators}
                userStatuses={userStatuses}
            />
            <ChatWindow
                conversation={activeConv}
                messages={activeMessages}
                currentUser={auth}
                onSend={handleSend}
                onAddMember={() => setModal('addMember')}
                typingUsers={activeKey ? (typingIndicators[activeKey] || []) : []}
                onTyping={() => activeConv && sendTypingSignal(activeConv.type, activeConv.id)}
                onDeleteMessage={(msgId) => activeConv && sendDeleteSignal(activeConv.type, activeConv.id, msgId)}
                hasMore={activeHasMore}
                onLoadMore={handleLoadMore}
                userStatuses={userStatuses}
            />

            <div className={`ws-indicator ${wsReady ? 'connected' : 'disconnected'}`}>
                {wsReady ? 'Connected' : 'Connecting...'}
            </div>
            {modal === 'direct' && <NewDirectModal onConfirm={(id, name) => {
                handleNewDirect(id, name);
                setModal(null);
            }} onClose={() => setModal(null)}/>}
            {modal === 'group' && <NewGroupModal onConfirm={(name) => {
                handleNewGroup(name).catch(console.error);
                setModal(null);
            }} onClose={() => setModal(null)}/>}
            {modal === 'addMember' && <AddMemberModal onConfirm={(id, name) => {
                handleAddMember(id, name).catch(console.error);
                setModal(null);
            }} onClose={() => setModal(null)}/>}
        </div>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <Toaster
                position="top-right"
                toastOptions={{
                    duration: 4000,
                    style: {
                        background: '#202c33',
                        color: '#e9edef',
                        border: '1px solid #2a3942'
                    },
                }}
            />
            <AppRouter />
        </AuthProvider>
    );
}

function AppRouter() {
    const { auth } = useAuth();
    return auth ? <MessagingApp /> : <AuthPage />;
}