# Tóm tắt thay đổi Client cho Spring Server

## 📋 Checklist nhanh

### 1. **Thay Colyseus → WebSocket thông thường**
```typescript
// ❌ Cũ
import { colyseusClient } from './colyseus';
const room = await colyseusClient.create('my_room', options);

// ✅ Mới  
import { websocketClient } from './websocket/websocketClient';
const room = await apiClient.post('/api/rooms/create', options);
await websocketClient.joinRoom(room.id, options);
```

### 2. **Thay tRPC → REST API**
```typescript
// ❌ Cũ
const maps = trpc.maps.getMapsToPlay.useQuery();

// ✅ Mới
const maps = useQuery({
  queryKey: ['maps', 'playable'],
  queryFn: () => apiClient.get('/api/maps/playable').then(r => r.data)
});
```

### 3. **Thay State Management**
```typescript
// ❌ Cũ
const state = useColyseusState((s) => s.players);

// ✅ Mới
const state = useGameStateStore((s) => s.state?.players);
```

### 4. **Thay Message Sending**
```typescript
// ❌ Cũ
room?.send('move', { x, y, rotation });

// ✅ Mới
websocketClient.send('move', { x, y, rotation });
```

### 5. **Thay Message Handlers**
```typescript
// ❌ Cũ
useRoomMessageHandler('waveStart', (message) => { ... });

// ✅ Mới
useEffect(() => {
  const handler = (message) => { ... };
  websocketClient.on('waveStart', handler);
  return () => websocketClient.off('waveStart', handler);
}, []);
```

---

## 🔧 Files cần thay đổi

### Core (Priority 1 - Phải làm trước):
1. `client/src/colyseus.ts` → Xóa, thay bằng WebSocket client
2. `client/src/lib/networking/hooks.ts` → Update message handlers
3. `client/src/lib/networking/rooms.ts` → Update room methods
4. `client/src/lib/trpc/trpcClient.ts` → Thay bằng REST API client

### Components (Priority 2):
5. `client/src/components/player/PlayerSelf.tsx` - Movement
6. `client/src/components/player/GunManager.tsx` - Shooting
7. `client/src/components/zombies/Zombies.tsx` - Zombie updates
8. `client/src/components/ui/Chat.tsx` - Chat
9. `client/src/components/ui/Menu.tsx` - Map selection
10. `client/src/components/level/useRemoteLevel.ts` - Map loading

### Auth (Priority 2):
11. `client/src/lib/auth/colyseusAuth.ts` → Update auth flow

---

## 📦 Dependencies cần thay đổi

### Xóa:
```json
{
  "@p3ntest/use-colyseus": "^0.0.9",
  "@trpc/client": "next",
  "@trpc/react-query": "next"
}
```

### Thêm:
```json
{
  "socket.io-client": "^4.5.0",  // hoặc WebSocket native
  "axios": "^1.6.0"  // nếu chưa có
}
```

---

## 🎯 Spring Server cần implement

### WebSocket:
- Endpoint: `/ws/game` 
- Protocol: Socket.IO hoặc STOMP
- Authentication: JWT trong handshake

### REST Endpoints:
- `POST /api/rooms/create`
- `POST /api/rooms/quick-play`  
- `GET /api/maps/{id}`
- `GET /api/maps/playable`
- `POST /api/maps`
- `GET /api/stats/leaderboard`
- `POST /api/assets/upload`

---

## ⚡ Quick Start Guide

1. **Tạo WebSocket client** (`websocketClient.ts`)
2. **Tạo REST API client** (`apiClient.ts`)
3. **Tạo Game State store** (Zustand)
4. **Update App.tsx** - thay auth hook
5. **Update MainStage.tsx** - subscribe to state
6. **Update từng component** - thay Colyseus calls

---

## 📝 Notes

- Giữ nguyên game logic (physics, rendering, etc.)
- Chỉ thay đổi networking layer
- Test từng component sau khi update
- Có thể giữ Colyseus types để reference




