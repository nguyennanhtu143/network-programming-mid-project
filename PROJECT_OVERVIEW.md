## Tổng quan dự án Zombies Multiplayer

Dự án gồm 2 phần chính: client (React + Vite + TypeScript) và server (Node.js/TypeScript, Colyseus, Express, tRPC, Prisma, PostgreSQL). Mục tiêu là tạo game co-op chống lại các wave zombies với backend realtime.

### Thành phần trong repository
- **Root**
  - `README.md`: mô tả ngắn về game và link demo.
- **client/**
  - React + TypeScript + Vite.
  - `client/README.md`: hướng dẫn mở rộng ESLint, plugin React.
- **server/**
  - Node.js/TypeScript, Colyseus server, Express, tRPC, Prisma.
  - `server/README.md`: scaffold từ create-colyseus-app.
  - `src/index.ts`: entrypoint khởi động Colyseus tools.
  - `src/app.config.ts`: cấu hình game server, Express routes, tRPC, upload, monitor.
  - `src/rooms/`: logic phòng chơi (Colyseus rooms) và state schema.
  - `src/prisma.ts`: khởi tạo PrismaClient.
  - `prisma/schema.prisma`: schema Postgres qua Prisma ORM.
  - `prisma/migrations/`: migration SQL.

## Client: thành phần core
- **Vite + React + TypeScript**: nền tảng build và HMR.
- **ESLint cấu hình type-aware (khuyến nghị)**: theo README client để nâng chất lượng code.

Lưu ý: README client mang tính template; phần gameplay UI, network client, asset… không mô tả ở README, nhưng sẽ giao tiếp với server realtime/HTTP theo thiết kế của server.

## Server: thành phần core
- **Colyseus**: framework realtime dựa trên WebSocket
  - `src/app.config.ts`: đăng ký room `my_room` và gắn `@colyseus/monitor`, `@colyseus/playground` (dev).
  - `src/rooms/MyRoom.ts` và `src/rooms/schema/MyRoomState.ts`: core gameplay logic, state đồng bộ tới client.
- **Express**: middleware HTTP
  - Static serve client build (prod), route `/colyseus` (monitor), `/playground` (dev), route upload `/createAsset`.
- **tRPC**: API type-safe gắn vào `/trpc` dùng `createContext` để suy ra user từ request.
- **Upload**: `express-fileupload` với giới hạn 10MB, luồng `handleAssetUpload` gắn với user.
- **Prisma ORM + PostgreSQL**
  - `prisma/schema.prisma`: models `User`, `Map`, `PlayedGame`, `PlayedGameParticipant`, `CustomAsset`.
  - `src/prisma.ts`: `PrismaClient` truy cập DB qua `DATABASE_URL`.
  - Migrations nằm ở `server/prisma/migrations/*`.

## Đánh giá migrate server sang Java Spring Boot

### Bức tranh tổng thể
- Bạn sẽ cần thay thế toàn bộ runtime Node/TypeScript bằng Spring Boot (Java 17+), đồng thời map từng lớp chức năng tương đương.
- Mức độ khó: **Trung bình-khá** tùy độ phức tạp logic trong `MyRoom.ts`. Phần khó nhất là thay thế cơ chế room/state realtime của Colyseus.

### Mapping chức năng
- **Realtime (Colyseus rooms)** → Spring WebSocket (STOMP) hoặc Netty/Undertow
  - Quản lý phòng, state sync, broadcast: dùng `SimpMessagingTemplate`, topic per room, và cơ chế state diff hoặc event-driven.
  - Cần tự định nghĩa protocol (payload JSON/CBOR), join/leave room, reconciliation/tick.
- **HTTP routes (Express)** → Spring MVC (`@RestController`)
  - `/trpc` → thay bằng REST/GraphQL (tRPC không có bản tương đương native trong JVM).
  - `/createAsset` upload → Spring `MultipartFile`, giới hạn size cấu hình `spring.servlet.multipart.max-file-size`.
  - Static serving → Spring ResourceHandler hoặc tách Nginx.
- **Auth/context (`createContext`, `extractUserFromRequest`)** → Spring Security
  - Filter chain, `OncePerRequestFilter`, JWT/OAuth2 Resource Server, method security `@PreAuthorize`.
- **ORM (Prisma)** → JPA/Hibernate + Spring Data
  - Entities cho `User`, `Map`, `PlayedGame`, `PlayedGameParticipant`, `CustomAsset`.
  - Migration: Flyway/Liquibase (dịch từ Prisma migrations hoặc rebuild schema).
  - Kết nối: `spring.datasource.url` tương đương `DATABASE_URL`.
- **File storage**
  - Hiện tại upload qua HTTP và lưu (chưa rõ storage). Trong Spring: lưu local, S3, hay dịch vụ khác.

### Rủi ro/khoảng trống
- **Colyseus state sync & protocol**: Colyseus có serialization/state sync tối ưu. Port sang Spring cần tự thiết kế loop/tick và diff/patch state hoặc chọn kiến trúc event-driven. Đây là phần tốn effort nhất.
- **tRPC → REST/GraphQL**: thay đổi kiểu gọi API, cần refactor client tương ứng (nếu client đang dùng tRPC client).
- **Hiệu năng WebSocket**: Spring WebSocket (STOMP) đủ tốt nhưng cần benchmark đối với tick rate cao của game. Có thể cân nhắc Netty thuần, Vert.x, hay Quarkus nếu cần latency thấp hơn.
- **Transaction/locking**: port logic ghi/đọc DB trong gameplay (nếu có) sang JPA cần lưu ý lazy loading, N+1, isolation.

### Khối lượng công việc (ước lượng tương đối)
- Thiết kế lại layer realtime (room, messaging, state): 40–80 giờ tùy độ phức tạp.
- Port API HTTP (upload, endpoints tRPC): 8–24 giờ.
- ORM & migrations: 16–32 giờ (tùy mapping và dữ liệu).
- Bảo mật (auth, context): 8–24 giờ.

## Kết luận
- Client ít phụ thuộc server framework, chủ yếu cần cập nhật cách gọi API nếu bỏ tRPC và giao thức WebSocket nếu thay protocol.
- Server migrate sang Spring Boot khả thi nhưng cần thiết kế lại phần realtime thay Colyseus; đây là chi phí lớn nhất. ORM, HTTP, upload, security đều có stack tương đương rõ ràng trong Spring.



