package com.project.server.springboot.websocket;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class GameRoom {
    private String id;
    private Map<String, Object> state;
    
    // Room configuration
    private boolean isPrivate;
    private int maxPlayers;
    private String waveStartType; // "manual" or "playerCount"
    private int requiredPlayerCount;
    private String mapId;
    
    // Track players in room
    private Set<String> playerSessions = ConcurrentHashMap.newKeySet();
    
    public GameRoom(String id) {
        this.id = id;
        this.state = new ConcurrentHashMap<>();
        
        // Initialize game state structure
        this.state.put("players", new ConcurrentHashMap<>());
        this.state.put("zombies", new ConcurrentHashMap<>());
        this.state.put("bullets", new ConcurrentHashMap<>());
        this.state.put("mapId", "map1");
        this.state.put("wave", 0);
        this.state.put("gameState", "waiting"); // waiting, playing, ended
        
        // Default configuration
        this.isPrivate = false;
        this.maxPlayers = 6;
        this.waveStartType = "manual";
        this.requiredPlayerCount = 1;
        this.mapId = null;
    }
    
    /**
     * Initialize room with configuration options
     */
    public void initialize(Map<String, Object> options) {
        if (options.containsKey("isPrivate")) {
            this.isPrivate = (Boolean) options.get("isPrivate");
        }
        if (options.containsKey("maxPlayers")) {
            this.maxPlayers = ((Number) options.get("maxPlayers")).intValue();
        }
        if (options.containsKey("waveStartType")) {
            this.waveStartType = (String) options.get("waveStartType");
        }
        if (options.containsKey("requiredPlayerCount")) {
            this.requiredPlayerCount = ((Number) options.get("requiredPlayerCount")).intValue();
        }
        if (options.containsKey("mapId")) {
            Object mapIdObj = options.get("mapId");
            if (mapIdObj != null) {
                this.mapId = mapIdObj.toString();
                this.state.put("mapId", this.mapId);
            }
        }
    }
    
    /**
     * Add player to room with player info
     */
    public boolean addPlayer(String sessionId, String playerName, String playerClass) {
        if (playerSessions.size() >= maxPlayers) {
            return false;
        }
        playerSessions.add(sessionId);
        
        // Create player state
        @SuppressWarnings("unchecked")
        Map<String, Object> players = (Map<String, Object>) state.get("players");
        Map<String, Object> playerState = new ConcurrentHashMap<>();
        playerState.put("sessionId", sessionId);
        playerState.put("name", playerName != null && !playerName.isEmpty() 
            ? playerName 
            : "Player " + sessionId.substring(0, Math.min(4, sessionId.length())));
        playerState.put("playerClass", playerClass != null ? playerClass : "pistol");
        playerState.put("x", (int) Math.floor(Math.random() * 800));
        playerState.put("y", (int) Math.floor(Math.random() * 600));
        playerState.put("health", 100);
        playerState.put("rotation", 0.0);
        playerState.put("velocityX", 0.0);
        playerState.put("velocityY", 0.0);
        playerState.put("finishedLoading", false);
        playerState.put("damageDealt", 0);
        playerState.put("kills", 0);
        playerState.put("deaths", 0);
        playerState.put("score", 0);
        
        players.put(sessionId, playerState);
        
        log.info("Player {} ({}) added to room {}", playerState.get("name"), sessionId, id);
        return true;
    }
    
    /**
     * Add player to room (backward compatibility)
     */
    public boolean addPlayer(String sessionId) {
        return addPlayer(sessionId, null, null);
    }
    
    /**
     * Remove player from room
     */
    public void removePlayer(String sessionId) {
        playerSessions.remove(sessionId);
        
        // Remove player from state
        @SuppressWarnings("unchecked")
        Map<String, Object> players = (Map<String, Object>) state.get("players");
        Object player = players.remove(sessionId);
        if (player != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> playerMap = (Map<String, Object>) player;
            String playerName = (String) playerMap.getOrDefault("name", "Unknown");
            log.info("Player {} ({}) removed from room {}", playerName, sessionId, id);
        }
    }
    
    /**
     * Get current player count
     */
    public int getPlayerCount() {
        return playerSessions.size();
    }
    
    /**
     * Check if room is available for joining (public, not full, not started)
     */
    public boolean isAvailableForJoin() {
        String gameState = (String) state.get("gameState");
        return !isPrivate 
            && getPlayerCount() < maxPlayers 
            && "waiting".equals(gameState);
    }
    
    /**
     * Handle game messages
     * Implement your game logic here
     */
    public void handleMessage(String type, Object data, String sessionId) {
        log.debug("Room {} handling message type: {} from session: {}", id, type, sessionId);
        
        // TODO: Implement your game message handling logic
        // Examples:
        // - "playerMove": Update player position
        // - "playerShoot": Handle shooting
        // - "playerHit": Handle damage
        // - "finishedLoading": Player ready
        // etc.
        
        switch (type) {
            case "finishedLoading":
                // Handle player finished loading
                break;
            default:
                log.warn("Unknown message type: {}", type);
        }
    }
    
    /**
     * Get current game state
     * This is sent to clients on stateUpdate events
     */
    public Map<String, Object> getState() {
        return state;
    }
}

