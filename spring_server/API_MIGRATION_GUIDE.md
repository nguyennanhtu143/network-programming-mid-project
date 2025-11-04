# API Migration Guide: Node.js/tRPC → Spring Boot/REST

Tài liệu này mô tả sự khác biệt giữa API endpoints của server cũ (Node.js + tRPC) và server mới (Spring Boot + REST) để hỗ trợ việc migrate client.

## Tổng quan thay đổi

| Aspect | Old (Node.js) | New (Spring Boot) |
|--------|---------------|-------------------|
| Protocol | tRPC (HTTP POST) | REST (HTTP verbs) |
| Base URL | `/trpc` | `/api` |
| WebSocket | Colyseus native protocol | STOMP over WebSocket |
| Auth Header | `Authorization: Bearer <token>` | `Authorization: Bearer <token>` (không đổi) |

---

## REST API Endpoints

### 1. Maps API

#### Load Map
**Old (tRPC):**
```typescript
client.maps.loadMap.query("map-id-123")
```

**New (REST):**
```typescript
GET /api/maps/load/map-id-123
// Response: { id, level, name, verified, published }
```

---

#### Verify Map
**Old (tRPC):**
```typescript
client.maps.verifyMap.mutate({ mapId: "map-id", verify: true })
```

**New (REST):**
```typescript
POST /api/maps/verify
Body: { "mapId": "map-id", "verify": true }
Headers: { "Authorization": "Bearer <token>" }
// Response: "Map {name} has been verified"
```

---

#### My Maps
**Old (tRPC):**
```typescript
client.maps.myMaps.query()
```

**New (REST):**
```typescript
GET /api/maps/my-maps
Headers: { "Authorization": "Bearer <token>" }
// Response: [ { id, level, name, verified, published }, ... ]
```

---

#### Get My Map One
**Old (tRPC):**
```typescript
client.maps.myMapOne.query("map-id")
```

**New (REST):**
```typescript
GET /api/maps/my-maps/map-id
Headers: { "Authorization": "Bearer <token>" }
```

---

#### Set Publish Map
**Old (tRPC):**
```typescript
client.maps.setPublishMap.mutate({ mapId: "map-id", publish: true })
```

**New (REST):**
```typescript
POST /api/maps/publish
Body: { "mapId": "map-id", "publish": true }
Headers: { "Authorization": "Bearer <token>" }
```

---

#### Overwrite Map
**Old (tRPC):**
```typescript
client.maps.overwriteMap.mutate({ mapId: "map-id", level: {...} })
```

**New (REST):**
```typescript
PUT /api/maps/overwrite
Body: { "mapId": "map-id", "level": {...} }
Headers: { "Authorization": "Bearer <token>" }
```

---

#### Delete Map
**Old (tRPC):**
```typescript
client.maps.deleteMap.mutate("map-id")
```

**New (REST):**
```typescript
DELETE /api/maps/map-id
Headers: { "Authorization": "Bearer <token>" }
```

---

#### Save New Map
**Old (tRPC):**
```typescript
client.maps.saveNewMap.mutate({ name: "My Map", level: {...} })
```

**New (REST):**
```typescript
POST /api/maps/save-new
Body: { "name": "My Map", "level": {...} }
Headers: { "Authorization": "Bearer <token>" }
```

---

#### Get Maps to Play
**Old (tRPC):**
```typescript
client.maps.getMapsToPlay.query()
```

**New (REST):**
```typescript
GET /api/maps/to-play
Headers: { "Authorization": "Bearer <token>" } (optional)
// Response: { verifiedMaps: [...], myMaps: [...], communityMaps: [...] }
```

---

### 2. Stats API

#### Get Leaderboard
**Old (tRPC):**
```typescript
client.stats.getLeaderboard.query()
```

**New (REST):**
```typescript
GET /api/stats/leaderboard
// Response: { leaderboard: [ {...}, ... ] }
```

---

### 3. Assets API

#### Assets Endpoint
**Old (tRPC):**
```typescript
client.maps.assets.assetsEndpoint.query()
```

**New (REST):**
```typescript
GET /api/assets/endpoint
// Response: "http://assets-service-url"
```

---

#### View Asset Library
**Old (tRPC):**
```typescript
client.maps.assets.viewAssetLibrary.query({ search: "zombie" })
```

**New (REST):**
```typescript
GET /api/assets/library?search=zombie
Headers: { "Authorization": "Bearer <token>" }
// Response: [ { id, uploadId, name, tags }, ... ]
```

---

#### Upload Asset from URL
**Old (tRPC):**
```typescript
client.maps.assets.uploadAssetFromUrl.mutate({ 
  externalUrl: "https://...", 
  name: "asset-name" 
})
```

**New (REST):**
```typescript
POST /api/assets/upload-from-url
Body: { "externalUrl": "https://...", "name": "asset-name" }
Headers: { "Authorization": "Bearer <token>" }
```

---

#### Create Asset (File Upload)
**Old (HTTP):**
```typescript
POST /createAsset
Content-Type: multipart/form-data
Body: file, assetName
```

**New (REST):**
```typescript
POST /api/assets/create
Content-Type: multipart/form-data
Body: file (MultipartFile), assetName (String)
Headers: { "Authorization": "Bearer <token>" }
```

---

### 4. Room Management

**New endpoints (không có tương đương trong old server):**

```typescript
// Tạo room
POST /api/rooms/create
Body: { "mapId": "...", "maxPlayers": 6 }
// Response: { "roomId": "abc12", "mapId": "..." }

// Thông tin room
GET /api/rooms/{roomId}
// Response: { "roomId", "mapId", "playerCount", "maxClients" }

// Tìm room available
GET /api/rooms/available
// Response: { "roomId": "abc12", "mapId": "..." }
```

---

## WebSocket API

### Connection

**Old (Colyseus):**
```typescript
const client = new Colyseus.Client("ws://localhost:2567");
const room = await client.joinOrCreate("my_room", options);

room.onMessage("chatMessage", (message) => { ... });
room.send("move", { x, y, rotation });
```

**New (STOMP over WebSocket):**
```typescript
const socket = new SockJS("http://localhost:2567/ws");
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to broadcasts
  stompClient.subscribe("/topic/room/{roomId}/chat", (message) => {
    const data = JSON.parse(message.body);
    console.log(data.message, data.color);
  });
  
  // Subscribe to personal messages
  stompClient.subscribe("/queue/player/{sessionId}/state", (message) => {
    const state = JSON.parse(message.body);
  });
  
  // Send messages
  stompClient.send("/app/room/{roomId}/join", {}, JSON.stringify({
    playerName: "Player1",
    userId: "user-id"
  }));
  
  stompClient.send("/app/room/{roomId}/move", {}, JSON.stringify({
    x: 100, y: 200, rotation: 0.5
  }));
});
```

---

### WebSocket Messages

| Old Message | New Destination | Direction |
|-------------|-----------------|-----------|
| `room.send("move", data)` | `/app/room/{roomId}/move` | Client → Server |
| `room.send("chatMessage", msg)` | `/app/room/{roomId}/chat` | Client → Server |
| `room.send("shoot", data)` | `/app/room/{roomId}/shoot` | Client → Server |
| `room.send("finishedLoading", data)` | `/app/room/{roomId}/finishedLoading` | Client → Server |
| `room.onMessage("chatMessage")` | `/topic/room/{roomId}/chat` | Server → Client |
| `room.onMessage("gameTick")` | `/topic/room/{roomId}/gameTick` | Server → Client |
| `room.onStateChange()` | `/queue/player/{sessionId}/state` | Server → Client |

---

## State Sync

**Old (Colyseus Schema):**
- Tự động sync state changes qua Colyseus Schema
- Client nhận delta patches
- `room.state.players`, `room.state.zombies`, etc.

**New (Manual JSON Broadcast):**
- State changes được broadcast thủ công dưới dạng JSON
- Client cần subscribe các topic:
  - `/topic/room/{roomId}/gameTick` - nhận game tick
  - `/topic/room/{roomId}/playerUpdate` - nhận player updates
  - `/topic/room/{roomId}/zombieUpdate` - nhận zombie updates
- Client cần tự quản lý local state và merge updates

---

## Authentication

**Không đổi:**
- Vẫn dùng JWT token trong `Authorization: Bearer <token>` header
- JWT issuer và JWKS endpoint giữ nguyên
- Scope permissions vẫn được lưu trong token

**Trong WebSocket (new):**
- Token có thể truyền qua STOMP headers khi connect:
```typescript
stompClient.connect({ 
  Authorization: "Bearer <token>" 
}, onConnect);
```

---

## Error Handling

**Old (tRPC):**
```typescript
try {
  await client.maps.loadMap.query("id");
} catch (error) {
  // TRPCError
}
```

**New (REST):**
```typescript
const response = await fetch("/api/maps/load/id");
if (!response.ok) {
  // HTTP status codes: 401, 403, 404, 500, etc.
  throw new Error(await response.text());
}
const data = await response.json();
```

---

## Client Migration Checklist

- [ ] Thay tRPC client bằng HTTP fetch/axios
- [ ] Cập nhật base URL từ `/trpc` sang `/api`
- [ ] Chuyển `.query()` thành `GET`, `.mutate()` thành `POST/PUT/DELETE`
- [ ] Thay Colyseus client bằng STOMP over WebSocket
- [ ] Cập nhật message handlers từ `room.onMessage()` sang `stompClient.subscribe()`
- [ ] Cập nhật message sends từ `room.send()` sang `stompClient.send()`
- [ ] Implement manual state sync logic thay vì dùng Colyseus Schema
- [ ] Test authentication flow với JWT
- [ ] Test WebSocket reconnection
- [ ] Cập nhật error handling

---

## Example: Full Migration

**Old Client Code (tRPC + Colyseus):**
```typescript
// API call
const maps = await trpcClient.maps.getMapsToPlay.query();

// WebSocket
const room = await colyseusClient.joinOrCreate("my_room", {
  playerName: "Player1"
});

room.onMessage("chatMessage", (data) => {
  console.log(data.message);
});

room.send("move", { x: 100, y: 200 });
```

**New Client Code (REST + STOMP):**
```typescript
// API call
const response = await fetch("/api/maps/to-play", {
  headers: { "Authorization": "Bearer " + token }
});
const maps = await response.json();

// WebSocket
const socket = new SockJS("/ws");
const stomp = Stomp.over(socket);

stomp.connect({ Authorization: "Bearer " + token }, () => {
  // Subscribe
  stomp.subscribe("/topic/room/" + roomId + "/chat", (msg) => {
    const data = JSON.parse(msg.body);
    console.log(data.message);
  });
  
  // Send
  stomp.send("/app/room/" + roomId + "/move", {}, JSON.stringify({
    x: 100, y: 200
  }));
});
```

---

## Support

Nếu gặp vấn đề trong quá trình migrate, tham khảo:
- `spring_server/README.md` - Hướng dẫn setup server
- Spring Boot docs: https://spring.io/guides
- STOMP protocol: https://stomp.github.io/


