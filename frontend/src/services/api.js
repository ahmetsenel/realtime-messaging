import axios from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
});

api.interceptors.request.use(
  (config) => {
    const auth = localStorage.getItem('auth');
    if (auth) {
      const { token } = JSON.parse(auth);
      if (token && token !== 'undefined' && token !== 'null')
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
    (response) => {
      if (response.data && response.data.hasOwnProperty('data')) {
        response.data = response.data.data;
      }
      return response;
    },
    (error) => {
      const requestUrl = error.config?.url || '';
      const isAuthRequest = requestUrl.includes('login') || requestUrl.includes('register');

      if (error.response?.status === 401 && !isAuthRequest) {
        localStorage.removeItem('auth');
        window.location.reload();
        return Promise.reject(error);
      }

      const message =
          error.response?.data?.message ||
          error.response?.data ||
          error.message ||
          'An error occurred while communicating with the server.';

      toast.error(message);

      return Promise.reject(new Error(message));
    }
);

export async function register(username, password) {
  const { data } = await api.post('/api/auth/register', {
    username,
    password,
    email: `${username}@msg.app`,
  });
  return enrichAuthResponse(data);
}

function decodeJwt(token) {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));
    return {
      userId: Number(decoded.sub),
      username: decoded.username,
    };
  } catch {
    return {};
  }
}

function enrichAuthResponse(data) {
  if (!data?.token) return data;
  const fromJwt = decodeJwt(data.token);
  return {
    ...data,
    userId:   data.userId   ?? fromJwt.userId,
    username: data.username ?? fromJwt.username,
  };
}

export async function login(username, password) {
  const { data } = await api.post('/api/auth/login', { username, password });
  return enrichAuthResponse(data);
}

export async function getMyDirectMessages() {
  const { data } = await api.get('/api/chat/direct');
  return data;
}

export async function getDirectMessages(withUserId, page = 0, size = 50) {
  const { data } = await api.get(`/api/chat/direct/${withUserId}/messages?page=${page}&size=${size}`);
  return data;
}

export async function getMyGroups() {
  const { data } = await api.get('/api/chat/groups');
  return data;
}

export async function createGroup(name) {
  const { data } = await api.post('/api/chat/groups', { name, memberIds: [] });
  return data;
}

export async function addGroupMember(groupId, userId, username) {
  const { data } = await api.post(`/api/chat/groups/${groupId}/members`, { userId, username });
  return data;
}

export async function getGroupMessages(groupId, page = 0, size = 50) {
  const { data } = await api.get(`/api/chat/groups/${groupId}/messages?page=${page}&size=${size}`);
  return data;
}

export async function getActiveUsers() {
  const { data } = await api.get('/api/chat/users/online');
  return data;
}

export async function getUserStatuses() {
  const { data } = await api.get('/api/chat/users/statuses');
  return data;
}

export async function searchUsers(username) {
  const { data } = await api.get('/api/users/search', { params: { username } });
  return data;
}
