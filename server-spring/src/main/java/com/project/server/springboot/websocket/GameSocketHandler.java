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
                    if (mapId != null) {
                        room.setMapId(mapId);
                        room.getState().put("mapId", mapId);
                    }
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
                // Process game message (implement your logic here)
                room.handleMessage(type, data, client.getSessionId().toString());
                
                // Broadcast state update to all clients in room
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

