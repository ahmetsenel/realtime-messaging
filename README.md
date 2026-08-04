# 💬 Real-Time Messaging

> A production-grade **real-time messaging platform** built with a microservice architecture using Spring Boot, WebSocket (STOMP), and React. Supports one-on-one direct messaging and group chats with rich messaging features.

---

## ✨ Features

### 💬 Messaging
- **Direct messaging** — one-on-one real-time chat
- **Group chats** — create groups, add members, chat with multiple people
- **Message replies** — reply to any message with a click-to-scroll preview
- **Message deletion** — delete for everyone with a soft-delete tombstone

### 📬 Message Status
- **Single tick** — message sent
- **Double tick (grey)** — message delivered to recipient's device
- **Double tick (blue)** — message read by recipient
- Full per-user tracking in group chats (delivered/read by each member)

### ⌨️ Live Indicators
- **Typing indicator** — see when someone is typing in real time
- **Online / Offline status** — live presence tracking with last-seen timestamps

### 🔐 Security
- **RSA RS256 JWT** — tokens signed with a private key, verified with a public key; no service can forge a token
- **Gateway-level auth** — JWT verified once at the API Gateway, injected as headers downstream
- **WebSocket auth** — STOMP `CONNECT` frames validated independently with the same RSA public key

### 🖥️ Frontend
- WhatsApp / Telegram-inspired dark UI
- Conversation list with last message, sender name, unread badge, and smart timestamps (Today / Yesterday / weekday / date)
- Date dividers inside chat (Today, Yesterday, etc.)
- Infinite scroll — load older messages on scroll-up
- Real-time sound notification on new messages
- User search with debounced dropdown

---

## 🛠️ Technology Stack
 
| 🗂️ Layer | ⚙️ Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| WebSocket | Spring WebSocket + STOMP |
| Security | Spring Security + JJWT (RSA RS256) |
| Database | PostgreSQL + Spring Data JPA + Hibernate |
| Build | Maven (multi-module) |
| Frontend | React 18 + Axios + @stomp/stompjs + SockJS |
| Containerization | Docker + Docker Compose |
 
---

## 🏗️ Architecture Overview

```
                    ┌─────────────────────────────────────────┐
                    │         Client (React + STOMP)          │
                    └────────────────────┬────────────────────┘
                                         │ HTTP / WebSocket
                    ┌────────────────────▼────────────────────┐
                    │           API Gateway  :8080            │
                    │   RSA JWT validation · Route filtering  │
                    └─────────┬──────────────────────┬────────┘
                              │                      │
              ┌───────────────▼───┐          ┌───────▼──────────────┐
              │   Auth Service    │          │    Chat Service      │
              │     :8081         │          │      :8082           │
              │  Register / Login │          │  REST + WebSocket    │
              │  RSA JWT signing  │          │  STOMP broker        │
              │  User search      │          │  Message persistence │
              └───────────────────┘          │  Presence tracking   │
                                             └──────────────────────┘
                    ┌──────────────────────────────────────────┐
                    │           Eureka Server  :8761           │
                    │             Service Discovery            │
                    └──────────────────────────────────────────┘
```

---

## 🧩 Services
 
### 🔍 Eureka Server — Port 8761
Netflix Eureka service registry. All microservices register here on startup and discover each other by name instead of hardcoded URLs.
 
### 🚪 API Gateway — Port 8080
Built with **Spring Cloud Gateway** (WebFlux). Single entry point for all traffic.
 
- **RSA JWT validation** — verifies tokens signed by Auth Service using the public key; injects `X-User-Id` and `X-User-Name` headers into downstream requests
- **Route definitions** — `auth-service` routes are public; `chat-service` REST routes require a valid JWT; WebSocket (`/ws/**`) is passed through for STOMP-level authentication
- **CORS** handled centrally
### 🔑 Auth Service — Port 8081
Handles identity and user management.
 
| 🔧 Method | 📍 Path | 📝 Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Create a new user |
| POST | `/api/auth/login` | Validate credentials, return RSA-signed JWT |
| GET | `/api/users/search?username=` | Case-insensitive user search |
| GET | `/api/users/{id}` | Get user by ID |
 
JWT signing uses an RSA **private key** loaded from the filesystem; the gateway verifies using the corresponding **public key**.
 
### 💬 Chat Service — Port 8082
Core service handling all messaging logic, WebSocket sessions, and user presence.
 
**REST API:**
 
| 🔧 Method | 📍 Path | 📝 Description |
|--------|------|-------------|
| GET | `/api/chat/direct` | Direct message summaries (latest per peer) |
| GET | `/api/chat/direct/{userId}/messages` | Paginated direct message history |
| GET | `/api/chat/groups` | Group summaries with unread counts |
| GET | `/api/chat/groups/{id}/messages` | Paginated group message history |
| POST | `/api/chat/groups` | Create a new group |
| POST | `/api/chat/groups/{id}/members` | Add a member to a group |
| GET | `/api/chat/users/online` | List online peer user IDs |
| GET | `/api/chat/users/statuses` | Get last-seen timestamps for peers |
 
**WebSocket — STOMP publish destinations:**
 
| 📤 Destination | 📝 Description |
|-------------|-------------|
| `/app/chat.send` | Send DIRECT or GROUP message |
| `/app/chat.read` | Send read receipt |
| `/app/chat.deliver` | Send delivery receipt |
| `/app/chat.typing` | Send typing indicator |
| `/app/chat.delete` | Delete a message |
 
**WebSocket — STOMP subscriptions:**
 
| 📥 Topic | 📝 Description |
|-------|-------------|
| `/user/queue/messages` | Incoming direct messages |
| `/user/queue/read` | Read receipt events |
| `/user/queue/deliver` | Delivery receipt events |
| `/user/queue/typing` | Typing events (direct) |
| `/user/queue/delete` | Delete events (direct) |
| `/user/queue/group.added` | Notified when added to a group |
| `/topic/group/{id}` | Group messages |
| `/topic/group/{id}/read` | Group read receipts |
| `/topic/group/{id}/deliver` | Group delivery receipts |
| `/topic/group/{id}/typing` | Group typing indicators |
| `/topic/group/{id}/delete` | Group delete events |
| `/topic/user.status` | Online/offline status broadcasts |
 
### 📦 Common Lib
A shared Maven module containing:
- `ApiResponse<T>` — uniform REST response wrapper
- `ErrorResponse` — error response structure
- `GlobalExceptionHandler` — centralized exception handling via `@RestControllerAdvice`
- `BusinessException` + `MessageType` enum — typed domain exceptions with HTTP status codes
  
---
 
## 🐳 Running with Docker
 
The easiest way to run the entire stack — no local Java or Node installation needed.
 
### Prerequisites
- [Docker](https://www.docker.com/products/docker-desktop) & Docker Compose
### Start everything
 
```bash
git clone https://github.com/ahmetsenel/realtime-messaging.git
cd realtime-messaging
docker-compose up --build
```
 
Docker Compose starts the following containers:
 
| 🐳 Container | 🔌 Port | 📝 Description |
|-----------|------|-------------|
| `postgres` | 5433 | PostgreSQL database |
| `eureka-server` | 8761 | Service registry |
| `api-gateway` | 8080 | API Gateway + JWT auth |
| `auth-service` | — | User auth & JWT signing |
| `chat-service` | — | Messaging + WebSocket |
| `frontend` | 3000 | React app (served via Nginx) |
 
Once all containers are healthy, open **http://localhost:3000** in your browser.
 
> 💡 Check service registration at **http://localhost:8761** — wait until all services show `UP` before testing.
 
### Stop
 
```bash
docker-compose down
```
 
### Stop and remove data
 
```bash
docker-compose down -v
```
 
---
 
## 💻 Running Locally (without Docker)
 
### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL 15+
- Node.js 18+
### 1. Database Setup
 
All services share a single PostgreSQL database (`postgres`). Just make sure PostgreSQL is running and the `postgres` database exists (it is created by default). Update the connection details in each service's `application.yml` if needed:
 
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: your_password
```
 
### 2. RSA Keys
 
RSA keys are already embedded in the classpath of each service — no manual key generation needed.
 
### 3. Start Services (in order)
```bash
# 1. Eureka
cd backend/eureka-server && mvn spring-boot:run
 
# 2. Auth Service
cd backend/auth-service && mvn spring-boot:run
 
# 3. Chat Service
cd backend/chat-service && mvn spring-boot:run
 
# 4. API Gateway
cd backend/api-gateway && mvn spring-boot:run
```
 
### 4. Frontend
```bash
cd frontend
npm install
npm start
```
 
App runs at **http://localhost:3000**, API at **http://localhost:8080**.
 
---
 
## 🔑 Key Design Decisions
 
### RSA Key-Pair Authentication
Auth Service signs JWTs with an **RSA private key** (RS256). The API Gateway and Chat Service each hold only the **public key** for verification. No service other than Auth Service can forge a token — a significant security improvement over shared symmetric secrets (HS256).
 
### Gateway-Level JWT Validation
JWT verification happens once at the gateway. Downstream services trust the `X-User-Id` / `X-User-Name` headers injected by the gateway. The Chat Service additionally validates JWTs directly on WebSocket `CONNECT` frames via `WebSocketAuthInterceptor` since WebSocket upgrades bypass the gateway filter chain.
 
### Database-per-Service
Each service owns its schema and manages it independently, following the microservice database isolation principle.
 
### Message Delivery & Read Tracking
The `Message` entity tracks delivery and read state per user using `Set<Long>` fields (`deliveredToUsers`, `readByUsers`). For groups, a message is marked `delivered`/`read` only when the set size reaches `memberCount - 1`.
 
### User Presence
`UserPresenceServiceImpl` uses Spring's `SimpUserRegistry` to check live WebSocket connections. On connect/disconnect, presence is published to `/topic/user.status` and last-seen time is persisted to the `UserStatus` entity.
 
### Pagination
Message histories are returned in pages (default 50) ordered `DESC` by `sentAt`. The frontend requests older pages on scroll-up and merges them with in-memory messages, deduplicating by ID.
 
### Message Reply & Soft Delete
- `replyToId` field links a message to the one being replied to; the frontend resolves it locally for display with click-to-scroll.
- Deletion sets `deleted = true` without removing database records. A delete event is broadcast to all participants; the frontend replaces content with a tombstone placeholder.
---
 
## 📁 Project Structure
 
```
real-time-messaging/
│
├── backend/
│   ├── eureka-server/
│   ├── api-gateway/
│   │   ├── filter/JwtAuthFilter.java        # RSA JWT validation
│   │   └── config/GatewayConfig.java        # Route definitions
│   │
│   ├── auth-service/
│   │   ├── controller/                      # AuthController, UserController
│   │   ├── security/JwtUtil.java            # RSA token generation
│   │   └── repository/UserRepository.java
│   │
│   ├── chat-service/
│   │   ├── controller/
│   │   │   ├── ChatController.java          # REST endpoints
│   │   │   └── ChatWebSocketController.java # STOMP message handlers
│   │   ├── service/
│   │   │   ├── impl/ChatServiceImpl.java    # Message CRUD + read/delivery
│   │   │   ├── impl/GroupServiceImpl.java   # Group management
│   │   │   └── impl/WebSocketServiceImpl.java
│   │   ├── socket/
│   │   │   ├── WebSocketPublisher.java      # Centralized STOMP publishing
│   │   │   └── WebSocketEventListener.java  # Connect/disconnect events
│   │   ├── security/
│   │   │   ├── WebSocketAuthInterceptor.java
│   │   │   └── GatewayAuthFilter.java
│   │   └── entity/                          # Message, Group, GroupMember, UserStatus
│   │
│   └── common-lib/
│       ├── exception/                       # BusinessException, MessageType
│       └── response/                        # ApiResponse, GlobalExceptionHandler
│
├── frontend/
│   ├── hooks/useChatManager.js              # All chat state + WS logic
│   ├── services/api.js                      # Axios instance + interceptors
│   ├── services/websocket.js                # STOMP client
│   └── components/                          # Sidebar, ChatWindow, Modal, AuthPage
│
└── docker-compose.yml
```
 
---

## 👨‍💻 Author
 
Made with ❤️ by **Ahmet Şenel**
 
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue?style=flat&logo=linkedin)](https://linkedin.com/in/ahmetşenel)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-black?style=flat&logo=github)](https://github.com/ahmetsenel)

---
