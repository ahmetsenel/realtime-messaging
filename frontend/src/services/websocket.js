import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BASE = 'http://localhost:8080';
let chatClient = null;

export function connectChat(token, { onMessage, onReadReceipt, onTyping, onUserStatusChange, onDelete, onGroupAdded, onDeliver, onConnect, onDisconnect }) {
  chatClient = new Client({
    webSocketFactory: () => new SockJS(`${BASE}/ws`),
    connectHeaders: { Authorization: `Bearer ${token}` },

    reconnectDelay: 3000,
    onConnect: () => {
      chatClient.subscribe('/user/queue/messages', frame => onMessage?.(JSON.parse(frame.body)));
      chatClient.subscribe('/user/queue/read', frame => onReadReceipt?.(JSON.parse(frame.body)));
      chatClient.subscribe('/user/queue/typing', frame => onTyping?.(JSON.parse(frame.body)));
      chatClient.subscribe('/topic/user.status', frame => onUserStatusChange?.(JSON.parse(frame.body)));
      chatClient.subscribe('/user/queue/delete', frame => onDelete?.(JSON.parse(frame.body)));
      chatClient.subscribe('/user/queue/deliver', frame => onDeliver?.(JSON.parse(frame.body)));
      chatClient.subscribe('/user/queue/group.added', frame => onGroupAdded?.(JSON.parse(frame.body)));

      onConnect?.();
    },
    onDisconnect: () => onDisconnect?.(),
    onStompError: (frame) => console.error('STOMP error:', frame),
  });
  chatClient.activate();
  return chatClient;
}

export function subscribeGroup(groupId, onMessage, onReadReceipt, onTyping, onDeliver, onDelete) {
  if (!chatClient?.connected) return null;
  const msgSub = chatClient.subscribe(`/topic/group/${groupId}`, frame => onMessage?.(JSON.parse(frame.body)));
  const readSub = chatClient.subscribe(`/topic/group/${groupId}/read`, frame => onReadReceipt?.(JSON.parse(frame.body)));
  const typeSub = chatClient.subscribe(`/topic/group/${groupId}/typing`, frame => onTyping?.(JSON.parse(frame.body)));
  const deliverSub = chatClient.subscribe(`/topic/group/${groupId}/deliver`, frame => onDeliver?.(JSON.parse(frame.body)));
  const delSub = chatClient.subscribe(`/topic/group/${groupId}/delete`, frame => onDelete?.(JSON.parse(frame.body)));

  return { msgSub, readSub, typeSub, deliverSub, delSub };
}

export function sendDirectMessage(receiverId, receiverUsername, content, replyToId = null) {
  chatClient?.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({ type: 'DIRECT', content, receiverId, receiverUsername, replyToId }),
  });
}

export function sendGroupMessage(groupId, content, replyToId = null) {
  chatClient?.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({ type: 'GROUP', content, groupId, replyToId }),
  });
}

export function sendReadReceipt(type, id) {
  chatClient?.connected && chatClient.publish({
    destination: '/app/chat.read',
    body: JSON.stringify({ type, id }) });
}

export function sendTypingSignal(type, id) {
  chatClient?.connected && chatClient.publish({
    destination: '/app/chat.typing',
    body: JSON.stringify({ type, id }) });
}

export function sendDeliverySignal(type, id) {
  chatClient?.connected && chatClient.publish({
    destination: '/app/chat.deliver',
    body: JSON.stringify({ type, id })
  });
}

export function sendDeleteSignal(type, id, messageId) {
  chatClient?.connected && chatClient.publish({
    destination: '/app/chat.delete',
    body: JSON.stringify({ type, id, messageId }),
  });
}

export function disconnectAll() {
  chatClient?.deactivate();
  chatClient = null;
}