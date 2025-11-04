# Zombies Multiplayer Server - Spring Boot

Server backend cho game Zombies Multiplayer được port từ Node.js/Colyseus sang Java Spring Boot.

## Yêu cầu hệ thống

- **Java**: 17 hoặc cao hơn
- **Maven**: 3.6+ (hoặc dùng Maven wrapper đi kèm)
- **PostgreSQL**: 12+ (database)

## Cấu trúc dự án

```
spring_server/
├── src/main/java/dev/p3ntest/zombies/
│   ├── config/              # Cấu hình Spring (Security, WebSocket, Scheduling)
│   ├── controller/          # REST API controllers
│   ├── dto/                 # Data Transfer Objects
│   ├── entity/              # JPA entities (User, Map, PlayedGame, etc.)
│   ├── game/                # Game logic và state models
│   │   └── state/           # Game state POJOs
│   ├── repository/          # Spring Data JPA repositories
│   ├── service/             # Business logic services
│   └── websocket/           # WebSocket handlers và room management
├── src/main/resources/
│   ├── application.yml      # Cấu hình ứng dụng
│   └── db/migration/        # Flyway migrations
└── pom.xml                  # Maven dependencies
```

## Cài đặt

### 1. Clone repository

```bash
cd spring_server
```

### 2. Cấu hình Database

Tạo database PostgreSQL:

```sql
CREATE DATABASE "zombies-game-db";
CREATE USER app_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE "zombies-game-db" TO app_user;
```

### 3. Cấu hình biến môi trường

Tạo file `.env` hoặc set biến môi trường:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/zombies-game-db
DB_USERNAME=app_user
DB_PASSWORD=your_password

# JWT/OAuth2
JWT_ISSUER_URI=https://zombies-auth.p3ntest.dev/oidc
JWT_JWK_SET_URI=https://zombies-auth.p3ntest.dev/oidc/jwks

# Assets Service
ASSETS_SERVICE_URL=http://localhost:3000
ASSETS_SERVICE_TOKEN=your_token_here

# Server
PORT=2567
```

Hoặc chỉnh trực tiếp trong `src/main/resources/application.yml`.

### 4. Build và chạy

**Sử dụng Maven:**

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

**Hoặc chạy JAR file:**

```bash
java -jar target/zombies-server-1.0.0.jar
```

Server sẽ khởi động tại `http://localhost:2567`

## API Endpoints

### Public Endpoints

- `GET /actuator/health` - Health check
- `GET /api/maps/load/{mapId}` - Load map by ID
- `GET /api/maps/to-play` - Danh sách maps để chơi
- `GET /api/stats/leaderboard` - Bảng xếp hạng

### Protected Endpoints (cần JWT token)

**Maps:**
- `POST /api/maps/verify` - Verify map (cần quyền `verify:maps`)
- `GET /api/maps/my-maps` - Lấy maps của user
- `POST /api/maps/save-new` - Tạo map mới
- `PUT /api/maps/overwrite` - Cập nhật map
- `DELETE /api/maps/{mapId}` - Xóa map
- `POST /api/maps/publish` - Publish/unpublish map

**Assets:**
- `GET /api/assets/endpoint` - URL của assets service
- `GET /api/assets/library?search=...` - Tìm assets
- `POST /api/assets/upload-from-url` - Upload asset từ URL
- `POST /api/assets/create` - Upload asset file

**Rooms:**
- `POST /api/rooms/create` - Tạo phòng game
- `GET /api/rooms/{roomId}` - Thông tin phòng
- `GET /api/rooms/available` - Tìm phòng available

### WebSocket Endpoints

Kết nối: `ws://localhost:2567/ws`

**Messages:**
- `/app/room/{roomId}/join` - Join room
- `/app/room/{roomId}/move` - Cập nhật vị trí player
- `/app/room/{roomId}/chat` - Gửi chat message
- `/app/room/{roomId}/leave` - Leave room

**Subscriptions:**
- `/topic/room/{roomId}/chat` - Nhận chat messages
- `/topic/room/{roomId}/gameTick` - Nhận game tick updates
- `/queue/player/{sessionId}/state` - Nhận state riêng

## Database Migrations

Flyway tự động chạy migrations khi khởi động. Schema được định nghĩa tại:
```
src/main/resources/db/migration/V1__initial_schema.sql
```

## Monitoring

Spring Boot Actuator cung cấp các endpoint:
- `/actuator/health` - Trạng thái health
- `/actuator/info` - Thông tin app
- `/actuator/metrics` - Metrics

## Authentication

Server dùng JWT OAuth2 Resource Server để xác thực. Token lấy từ header:
```
Authorization: Bearer <jwt_token>
```

JWT được verify qua JWKS endpoint của auth server.

## So sánh với Node.js server

| Feature | Node.js (Colyseus) | Spring Boot |
|---------|-------------------|-------------|
| WebSocket | Colyseus native | STOMP over WebSocket |
| State Sync | Schema serialization | Manual JSON broadcast |
| ORM | Prisma | JPA/Hibernate |
| API | tRPC | REST |
| Auth | JWT verify inline | Spring Security OAuth2 |

## Phát triển

### Thêm game logic

Logic game nằm ở package `dev.p3ntest.zombies.game`. Để thêm features như zombie spawning, wave management, cần implement tại:
- `GameRoom.java` - Room instance logic
- `RoomManager.java` - Room lifecycle
- `game/state/` - State models

### Debug

Enable SQL logging trong `application.yml`:
```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

## Troubleshooting

**Database connection error:**
- Kiểm tra PostgreSQL đang chạy
- Xác nhận `DATABASE_URL`, username, password

**JWT verification failed:**
- Kiểm tra `JWT_ISSUER_URI` và `JWT_JWK_SET_URI`
- Đảm bảo token hợp lệ và chưa hết hạn

**WebSocket connection refused:**
- Kiểm tra CORS configuration trong `WebSocketConfig.java`
- Đảm bảo client connect đúng endpoint `/ws`

## License

MIT


