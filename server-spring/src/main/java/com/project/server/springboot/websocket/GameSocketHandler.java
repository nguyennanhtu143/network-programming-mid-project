package com.project.server.springboot.websocket;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.project.server.springboot.repository.MapRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for game rooms
 * Handles Socket.IO events from frontend
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSocketHandler {

    private final SocketIOServer socketIOServer;
    private final MapRepository mapRepository;
    private final ObjectMapper json = new ObjectMapper();
    
    // Store room state in memory
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    
    // Track client to room mapping
    private final Map<String, String> clientToRoom = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void start() {
        // Register this handler with SocketIOServer so @OnConnect, @OnEvent annotations work
        socketIOServer.addListeners(this);
        log.info("GameSocketHandler registered with SocketIOServer");
        
        socketIOServer.start();
        log.info("Socket.IO server started on port {}", socketIOServer.getConfiguration().getPort());
    }
    
    @PreDestroy
    public void stop() {
        socketIOServer.stop();
        log.info("Socket.IO server stopped");
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        log.info("Client connected: {}", client.getSessionId().toString());
        
        // Extract auth token from handshake if needed
        // String token = client.getHandshakeData().getSingleUrlParam("token");
        // You can validate token here and store user info in client
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        log.info("Client disconnected: {}", sessionId);
        
        // Remove player from room
        String roomId = clientToRoom.remove(sessionId);
        if (roomId != null) {
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                room.removePlayer(sessionId);
                
                // If room is empty, remove it (optional - you might want to keep it for a while)
                if (room.getPlayerCount() == 0) {
                    rooms.remove(roomId);
                    log.info("Removed empty room: {}", roomId);
                } else {
                    // Broadcast state update to remaining players
                    socketIOServer.getRoomOperations(roomId).sendEvent("stateUpdate", room.getState());
                }
            }
        }
    }

    @OnEvent("joinRoom")
    public void onJoinRoom(SocketIOClient client, Map<String, Object> data) {
        String sessionId = client.getSessionId().toString();
        String roomId = (String) data.get("roomId");
        log.info("Client {} joining room {}", sessionId, roomId);
        
        // Get or create game room
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            Map<String, Object> errorResponse = new ConcurrentHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Room not found");
            client.sendEvent("roomJoined", errorResponse);
            return;
        }
        
        // Check if room is available
        if (!room.isAvailableForJoin()) {
            Map<String, Object> errorResponse = new ConcurrentHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Room is not available (private, full, or already started)");
            client.sendEvent("roomJoined", errorResponse);
            return;
        }
        
        // Extract player info from data
        String playerName = (String) data.getOrDefault("playerName", null);
        String playerClass = (String) data.getOrDefault("playerClass", "pistol");
        
        // Add player to room with info
        if (!room.addPlayer(sessionId, playerName, playerClass)) {
            Map<String, Object> errorResponse = new ConcurrentHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Room is full");
            client.sendEvent("roomJoined", errorResponse);
            return;
        }
        
        // Track client to room mapping
        clientToRoom.put(sessionId, roomId);
        
        // Join Socket.IO room
        client.joinRoom(roomId);
        
        // Send initial state
        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("roomId", roomId);
        response.put("sessionId", sessionId);
        response.put("reconnectToken", generateReconnectToken(sessionId, roomId));
        response.put("state", room.getState());
        response.put("success", true);
        
        client.sendEvent("roomJoined", response);

        // Also send map data via WS so client doesn't need HTTP fetch
        trySendMapDataToClient(client, room.getMapId());
        
        // Broadcast state update to all players in room
        socketIOServer.getRoomOperations(roomId).sendEvent("stateUpdate", room.getState());
    }

    @OnEvent("createRoom")
    public void onCreateRoom(SocketIOClient client, Map<String, Object> options, AckRequest ackRequest) {
        String sessionId = client.getSessionId().toString();
        log.info("createRoom event received from client {} with options: {}", sessionId, options);
        log.info("AckRequest is null: {}, isAckRequested: {}", ackRequest == null, 
                ackRequest != null ? ackRequest.isAckRequested() : "N/A");
        
        try {
            Boolean quickPlay = (Boolean) options.getOrDefault("quickPlay", false);
            
            GameRoom room;
            String roomId;
            
            if (quickPlay) {
                // QuickPlay: Try to find existing room or create new one
                log.info("Client {} requesting quick play", sessionId);
                
                // Find available room for QuickPlay
                room = findAvailableQuickPlayRoom();
                
                if (room != null) {
                    // Join existing room
                    roomId = room.getId();
                    log.info("Client {} joining existing quick play room {}", sessionId, roomId);
                } else {
                    // Create new QuickPlay room
                    roomId = generateRoomId();
                    room = new GameRoom(roomId);
                    
                    // Configure QuickPlay room
                    Map<String, Object> quickPlayOptions = new ConcurrentHashMap<>();
                    quickPlayOptions.put("isPrivate", false);
                    quickPlayOptions.put("maxPlayers", 6);
                    quickPlayOptions.put("waveStartType", "playerCount");
                    quickPlayOptions.put("requiredPlayerCount", 2);
                    
                    // Random select verified map
                    String mapId = selectRandomVerifiedMap();
                    if (mapId != null) {
                        quickPlayOptions.put("mapId", mapId);
                    } else {
                        // Fallback to built-in default map id
                        quickPlayOptions.put("mapId", "map1");
                    }
                    
                    room.initialize(quickPlayOptions);
                    rooms.put(roomId, room);
                    
                    log.info("Client {} created new quick play room {}", sessionId, roomId);
                }
            } else {
                // Regular room creation
                roomId = generateRoomId();
                room = new GameRoom(roomId);
                room.initialize(options);
                
                // If no mapId specified, select random verified map
                if (room.getMapId() == null || room.getMapId().isEmpty()) {
                    String mapId = selectRandomVerifiedMap();
                    if (mapId == null || mapId.isEmpty()) {
                        mapId = "map1"; // fallback
                    }
                    room.setMapId(mapId);
                    room.getState().put("mapId", mapId);
                }
                
                rooms.put(roomId, room);
                log.info("Client {} creating room {}", sessionId, roomId);
            }
            
            // Extract player info from options
            String playerName = (String) options.getOrDefault("playerName", null);
            String playerClass = (String) options.getOrDefault("playerClass", "pistol");
            
            // Add player to room with info
            if (!room.addPlayer(sessionId, playerName, playerClass)) {
                // Room is full, send error
                Map<String, Object> errorResponse = new ConcurrentHashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Room is full");
                if (ackRequest != null && ackRequest.isAckRequested()) {
                    ackRequest.sendAckData(errorResponse);
                } else {
                    client.sendEvent("createRoom", errorResponse);
                }
                return;
            }
            
            // Track client to room mapping
            clientToRoom.put(sessionId, roomId);
            
            // Join Socket.IO room
            client.joinRoom(roomId);
            
            // Send response (same format as roomJoined) - use acknowledgment if requested
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("roomId", roomId);
            response.put("sessionId", sessionId);
            response.put("reconnectToken", generateReconnectToken(sessionId, roomId));
            response.put("state", room.getState());
            response.put("success", true);
            
            // Send acknowledgment if client expects it (from emit with callback)
            if (ackRequest != null && ackRequest.isAckRequested()) {
                ackRequest.sendAckData(response);
                log.info("Sent acknowledgment to client {} for createRoom", sessionId);
            } else {
                // Fallback: send as regular event
                client.sendEvent("roomJoined", response);
            }
            
            // Broadcast state update to all players in room
            socketIOServer.getRoomOperations(roomId).sendEvent("stateUpdate", room.getState());

            // Also send map data via WS so client doesn't need HTTP fetch
            trySendMapDataToClient(client, room.getMapId());
        
        } catch (Exception e) {
            log.error("Error in onCreateRoom for client {}: ", sessionId, e);
            Map<String, Object> errorResponse = new ConcurrentHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Server error: " + e.getMessage());
            if (ackRequest != null && ackRequest.isAckRequested()) {
                ackRequest.sendAckData(errorResponse);
            } else {
                client.sendEvent("createRoom", errorResponse);
            }
        }
    }
    
    /**
     * Find an available room for QuickPlay matching
     */
    private GameRoom findAvailableQuickPlayRoom() {
        return rooms.values().stream()
                .filter(GameRoom::isAvailableForJoin)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Select a random verified map from database
     */
    private String selectRandomVerifiedMap() {
        try {
            Optional<com.project.server.springboot.model.Map> randomMap = mapRepository.findRandomVerifiedMap();
            if (randomMap.isPresent()) {
                return randomMap.get().getId();
            }
            
            // Fallback: Get all verified maps and select one
            List<com.project.server.springboot.model.Map> verifiedMaps = mapRepository.findByVerifiedTrue();
            if (!verifiedMaps.isEmpty()) {
                int randomIndex = (int) (Math.random() * verifiedMaps.size());
                return verifiedMaps.get(randomIndex).getId();
            }
            
            log.warn("No verified maps found in database");
            return null;
        } catch (Exception e) {
            log.error("Error selecting random verified map", e);
            return null;
        }
    }

    /**
     * Try to load map from repository and send to the client over WS
     * Event name: "mapData"
     * Payload: { id, name, level }
     */
    private void trySendMapDataToClient(SocketIOClient client, String mapId) {
        try {
            String effectiveMapId = (mapId == null || mapId.isEmpty()) ? "map1" : mapId;
            var opt = mapRepository.findById(effectiveMapId);
            Map<String, Object> payload = new ConcurrentHashMap<>();
            if (opt.isPresent()) {
                var map = opt.get();
                payload.put("id", map.getId());
                payload.put("name", map.getName());
                Object level = map.getLevel();
                // Nếu DB lưu JSON dạng chuỗi, parse sang object trước khi gửi
                if (level instanceof String s) {
                    try {
                        level = json.readValue(s, java.util.Map.class);
                    } catch (Exception ignore) {
                        // nếu parse lỗi, vẫn gửi raw string (FE sẽ bỏ qua/đã có fallback)
                    }
                }
                // Chuẩn hoá: đảm bảo có field objects[] cho FE renderer
                if (level instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> lvl = (Map<String, Object>) level;
                    Object objects = lvl.get("objects");
                    if (objects == null) {
                        java.util.List<Map<String, Object>> objList = new java.util.ArrayList<>();
                        // chuyển spawnPoints -> objects spawnPoint player
                        Object sps = lvl.get("spawnPoints");
                        if (sps instanceof Iterable<?> it) {
                            for (Object o : it) {
                                if (o instanceof Map<?, ?> sp) {
                                    Object ox = sp.containsKey("x") ? sp.get("x") : Integer.valueOf(400);
                                    Object oy = sp.containsKey("y") ? sp.get("y") : Integer.valueOf(300);
                                    Number x = (ox instanceof Number) ? (Number) ox : Integer.valueOf(400);
                                    Number y = (oy instanceof Number) ? (Number) oy : Integer.valueOf(300);
                                    objList.add(Map.of(
                                            "id", "sp_p_" + x + "_" + y,
                                            "objectType", "spawnPoint",
                                            "spawns", "player",
                                            "x", x,
                                            "y", y
                                    ));
                                }
                            }
                        }
                        // chuyển zombieSpawnPoints -> objects spawnPoint zombie
                        Object zsps = lvl.get("zombieSpawnPoints");
                        if (zsps instanceof Iterable<?> it2) {
                            for (Object o : it2) {
                                if (o instanceof Map<?, ?> sp) {
                                    Object ox = sp.containsKey("x") ? sp.get("x") : Integer.valueOf(800);
                                    Object oy = sp.containsKey("y") ? sp.get("y") : Integer.valueOf(300);
                                    Number x = (ox instanceof Number) ? (Number) ox : Integer.valueOf(800);
                                    Number y = (oy instanceof Number) ? (Number) oy : Integer.valueOf(300);
                                    objList.add(Map.of(
                                            "id", "sp_z_" + x + "_" + y,
                                            "objectType", "spawnPoint",
                                            "spawns", "zombie",
                                            "x", x,
                                            "y", y
                                    ));
                                }
                            }
                        }
                        lvl.put("objects", objList);
                        level = lvl;
                    }
                }
                payload.put("level", level);
            } else {
                // Fallback default map (minimal valid structure)
                payload.put("id", effectiveMapId);
                payload.put("name", "Default Map");
                payload.put("level", Map.of(
                        "name", "Default",
                        "walls", java.util.List.of(),
                        "objects", java.util.List.of(
                                Map.of("id", "sp1", "objectType", "spawnPoint", "spawns", "player", "x", 400, "y", 300),
                                Map.of("id", "spz1", "objectType", "spawnPoint", "spawns", "zombie", "x", 800, "y", 300)
                        )
                ));
            }
            client.sendEvent("mapData", payload);
						log.info("Payload sent: {}", payload);
        } catch (Exception e) {
            log.warn("Failed to send map data for mapId {}: {}", mapId, e.getMessage());
        }
    }

    @OnEvent("reconnect")
    public void onReconnect(SocketIOClient client, Map<String, Object> data) {
        String token = (String) data.get("token");
        log.info("Client {} reconnecting with token {}", client.getSessionId().toString(), token);
        
        // Validate token and restore session (implement your logic here)
        // Parse token to get roomId and sessionId
        // Restore player state
        
        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("success", false);
        response.put("error", "Reconnect not implemented yet");
        
        client.sendEvent("reconnect", response);
    }

    @OnEvent("gameMessage")
    public void onGameMessage(SocketIOClient client, Map<String, Object> message) {
        String type = (String) message.get("type");
        Object data = message.get("data");
        
        // Get room from client
        String roomId = getRoomIdFromClient(client);
        if (roomId != null) {
            GameRoom room = rooms.get(roomId);
            if (room != null) {
                // Special handling for messages FE depends on
                if ("requestMap".equals(type)) {
                    // Client explicitly requests map data again
                    String requestedMapId = null;
                    if (data instanceof Map<?, ?> md) {
                        Object mid = md.get("mapId");
                        if (mid instanceof String s) requestedMapId = s;
                    }
                    String toSend = (requestedMapId != null && !requestedMapId.isEmpty())
                            ? requestedMapId
                            : (rooms.getOrDefault(roomId, room).getMapId() != null
                                ? rooms.getOrDefault(roomId, room).getMapId()
                                : "map1");
                    trySendMapDataToClient(client, toSend);
                } else if ("finishedLoading".equals(type)) {
                    // mark player as finishedLoading
                    @SuppressWarnings("unchecked")
                    Map<String, Object> players = (Map<String, Object>) room.getState().get("players");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> player = (Map<String, Object>) players.get(client.getSessionId().toString());
                    if (player != null) {
                        player.put("finishedLoading", true);
                    }
                    // send requestSpawn to this client
                    client.sendEvent("requestSpawn");
                    // if game not started, start wave 1
                    if ("waiting".equals(room.getState().get("gameState"))) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> waveInfo = (Map<String, Object>) room.getState().get("waveInfo");
                        if (waveInfo == null) {
                            waveInfo = new java.util.concurrent.ConcurrentHashMap<>();
                            waveInfo.put("currentWaveNumber", 0);
                            waveInfo.put("active", false);
                            waveInfo.put("nextWaveStartsInSec", 0);
                            waveInfo.put("totalZombies", 0);
                            waveInfo.put("zombiesLeft", 0);
                            room.getState().put("waveInfo", waveInfo);
                        }
                        waveInfo.put("currentWaveNumber", 1);
                        waveInfo.put("active", true);
                        waveInfo.put("totalZombies", 10);
                        waveInfo.put("zombiesLeft", 10);
                        room.getState().put("gameState", "playing");
                        // notify FE
                        socketIOServer.getRoomOperations(roomId).sendEvent("waveStart", java.util.Map.of("wave", 1));
                    }
                } else if ("spawnSelf".equals(type)) {
                    // Client chose a spawn point; set player position and mark alive
                    @SuppressWarnings("unchecked")
                    Map<String, Object> players = (Map<String, Object>) room.getState().get("players");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> player = (Map<String, Object>) players.get(client.getSessionId().toString());
                    if (player != null) {
                        Number x = 0, y = 0;
                        if (data instanceof Map<?, ?> md) {
                            Object ox = md.get("x");
                            Object oy = md.get("y");
                            x = (ox instanceof Number) ? (Number) ox : Integer.valueOf(400);
                            y = (oy instanceof Number) ? (Number) oy : Integer.valueOf(300);
                        }
                        player.put("x", x);
                        player.put("y", y);
                        player.put("healthState", 0); // PlayerHealthState.ALIVE
                        // make sure has non-zero health
                        Object h = player.get("health");
                        if (!(h instanceof Number) || ((Number) h).intValue() <= 0) {
                            player.put("health", 100);
                        }
                    }
                } else {
                    // Process other messages in room logic
                    room.handleMessage(type, data, client.getSessionId().toString());
                }
                // Broadcast updated state to all clients in room
                socketIOServer.getRoomOperations(roomId).sendEvent("stateUpdate", room.getState());
            }
        }
    }

    private String generateRoomId() {
        return "room_" + System.currentTimeMillis() + "_" + 
               (int)(Math.random() * 1000);
    }

    private String generateReconnectToken(String sessionId, String roomId) {
        return sessionId + "_" + roomId + "_" + System.currentTimeMillis();
    }

    private String getRoomIdFromClient(SocketIOClient client) {
        // First try to get from client mapping
        String sessionId = client.getSessionId().toString();
        String roomId = clientToRoom.get(sessionId);
        if (roomId != null) {
            return roomId;
        }
        
        // Fallback: Get first room that client joined
        return client.getAllRooms().stream()
                .filter(room -> room.startsWith("room_"))
                .findFirst()
                .orElse(null);
    }
}

