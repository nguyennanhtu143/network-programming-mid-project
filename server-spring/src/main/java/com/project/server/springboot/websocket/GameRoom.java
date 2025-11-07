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

    // Simple counters
    private int highestBulletId = 0;
    private int highestZombieId = 0;
    
    public GameRoom(String id) {
        this.id = id;
        this.state = new ConcurrentHashMap<>();
        
        // Initialize game state structure
        this.state.put("players", new ConcurrentHashMap<>());
        // FE mong đợi mảng: zombies[], bullets[]
        this.state.put("zombies", new java.util.ArrayList<>());
        this.state.put("bullets", new java.util.ArrayList<>());
        this.state.put("mapId", "map1");
        this.state.put("gameState", "waiting"); // waiting, playing, ended
        // Wave info structure expected by FE
        Map<String, Object> waveInfo = new ConcurrentHashMap<>();
        waveInfo.put("currentWaveNumber", 0);
        waveInfo.put("active", false);
        waveInfo.put("nextWaveStartsInSec", 0);
        waveInfo.put("totalZombies", 0);
        waveInfo.put("zombiesLeft", 0);
        this.state.put("waveInfo", waveInfo);
        
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
        playerState.put("rotation", 0.0);
        playerState.put("connected", true);
        playerState.put("velocityX", 0.0);
        playerState.put("velocityY", 0.0);
        playerState.put("health", 100);
        // 2 = NOT_SPAWNED (client constants)
        playerState.put("healthState", 2);
        playerState.put("skillPoints", 0);
        Map<String, Object> upgrades = new ConcurrentHashMap<>();
        upgrades.put("fireRate", 0);
        upgrades.put("damage", 0);
        upgrades.put("pierce", 0);
        upgrades.put("health", 0);
        upgrades.put("speed", 0);
        upgrades.put("scope", 0);
        playerState.put("upgrades", upgrades);
        playerState.put("kills", 0);
        playerState.put("deaths", 0);
        playerState.put("damageDealt", 0);
        playerState.put("wavesSurvived", 0);
        playerState.put("accuracy", 0);
        playerState.put("currentAnimation", 0);
        playerState.put("finishedLoading", false);
        
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
            case "finishedLoading": {
                // FE uses this only as readiness signal; nothing to change here for now
                break;
            }
            case "shoot": {
                // Expect: { originX, originY, rotation, speed, damage, pierces, knockBack }
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> bullets = (java.util.List<Map<String, Object>>) state.get("bullets");
                if (bullets == null) {
                    bullets = new java.util.ArrayList<>();
                    state.put("bullets", bullets);
                }
                Map<String, Object> bullet = new ConcurrentHashMap<>();
                if (data instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> md = (Map<String, Object>) data;
                    bullet.put("originX", md.getOrDefault("originX", Integer.valueOf(0)));
                    bullet.put("originY", md.getOrDefault("originY", Integer.valueOf(0)));
                    bullet.put("rotation", md.getOrDefault("rotation", Integer.valueOf(0)));
                    bullet.put("speed", md.getOrDefault("speed", Integer.valueOf(10)));
                    bullet.put("damage", md.getOrDefault("damage", Integer.valueOf(1)));
                    // keep piercesLeft like node version
                    Object p = md.getOrDefault("pierces", Integer.valueOf(1));
                    bullet.put("piercesLeft", (p instanceof Number) ? p : Integer.valueOf(1));
                    bullet.put("knockBack", md.getOrDefault("knockBack", Integer.valueOf(1)));
                }
                bullet.put("playerId", sessionId);
                bullet.put("id", ++highestBulletId);
                bullets.add(bullet);
                break;
            }
            case "destroyBullet": {
                // Expect: bullet id number
                if (data instanceof Number n) {
                    int idToRemove = n.intValue();
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> bullets = (java.util.List<Map<String, Object>>) state.get("bullets");
                    if (bullets != null) {
                        bullets.removeIf(b -> {
                            Object bid = b.get("id");
                            return (bid instanceof Number) && ((Number) bid).intValue() == idToRemove;
                        });
                    }
                }
                break;
            }
            case "move": {
                // Expect: { x, y, rotation, velocityX, velocityY, currentAnimation }
                @SuppressWarnings("unchecked")
                Map<String, Object> players = (Map<String, Object>) state.get("players");
                if (players == null) {
                    log.warn("Players map is null in room {}", id);
                    break;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> player = (Map<String, Object>) players.get(sessionId);
                if (player == null) {
                    log.warn("Player {} not found in room {}", sessionId, id);
                    break;
                }
                if (data instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> md = (Map<String, Object>) data;
                    Object ox = md.get("x");
                    Object oy = md.get("y");
                    Object orot = md.get("rotation");
                    Object ovx = md.get("velocityX");
                    Object ovy = md.get("velocityY");
                    Object oanim = md.get("currentAnimation");
                    if (ox instanceof Number) player.put("x", ox);
                    if (oy instanceof Number) player.put("y", oy);
                    if (orot instanceof Number) player.put("rotation", orot);
                    if (ovx instanceof Number) player.put("velocityX", ovx);
                    if (ovy instanceof Number) player.put("velocityY", ovy);
                    if (oanim != null) player.put("currentAnimation", oanim);
                    log.debug("Updated player {} position: x={}, y={}, rotation={}", sessionId, ox, oy, orot);
                } else {
                    log.warn("Move data is not a Map: {}", data);
                }
                break;
            }
            case "updateZombieBatch": {
                // Expect: Array of { id, x, y, rotation }
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> zombies = (java.util.List<Map<String, Object>>) state.get("zombies");
                if (zombies == null) {
                    log.warn("Zombies list is null in room {}", id);
                    break;
                }
                if (data instanceof java.util.List<?>) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> updates = (java.util.List<Map<String, Object>>) data;
                    for (Map<String, Object> update : updates) {
                        Object zombieId = update.get("id");
                        if (zombieId == null) continue;
                        
                        // Find zombie by id
                        for (Map<String, Object> zombie : zombies) {
                            Object zid = zombie.get("id");
                            if (zid != null && zid.equals(zombieId)) {
                                // Update zombie position/rotation
                                Object ox = update.get("x");
                                Object oy = update.get("y");
                                Object orot = update.get("rotation");
                                if (ox instanceof Number) zombie.put("x", ox);
                                if (oy instanceof Number) zombie.put("y", oy);
                                if (orot instanceof Number) zombie.put("rotation", orot);
                                break;
                            }
                        }
                    }
                    log.debug("Updated {} zombie positions from batch", updates.size());
                } else {
                    log.warn("updateZombieBatch data is not a List: {}", data);
                }
                break;
            }
            case "spawnZombie": {
                // Expect: { x, y, type, respawnId? }
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> zombies = (java.util.List<Map<String, Object>>) state.get("zombies");
                if (zombies == null) {
                    zombies = new java.util.ArrayList<>();
                    state.put("zombies", zombies);
                }
                
                if (data instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> spawnData = (Map<String, Object>) data;
                    
                    Object respawnIdObj = spawnData.get("respawnId");
                    if (respawnIdObj != null) {
                        // TODO: Handle respawn logic if needed
                        log.debug("Respawn zombie requested, but respawn not fully implemented yet");
                    }
                    
                    // Create new zombie
                    Map<String, Object> zombie = new ConcurrentHashMap<>();
                    zombie.put("id", ++highestZombieId);
                    
                    // Get zombie type (default to "normal")
                    String zombieType = (String) spawnData.getOrDefault("type", "normal");
                    zombie.put("zombieType", zombieType);
                    
                    // Get position
                    Object ox = spawnData.get("x");
                    Object oy = spawnData.get("y");
                    zombie.put("x", (ox instanceof Number) ? ox : Integer.valueOf(800));
                    zombie.put("y", (oy instanceof Number) ? oy : Integer.valueOf(300));
                    
                    // Initialize zombie properties
                    // Default values matching server/src/game/zombies.ts
                    int baseHealth = 100;
                    if ("baby".equals(zombieType)) baseHealth = 30;
                    else if ("mutatedBaby".equals(zombieType)) baseHealth = 50;
                    else if ("greenMutant".equals(zombieType)) baseHealth = 200;
                    else if ("tank".equals(zombieType)) baseHealth = 600;
                    else if ("blueMutant".equals(zombieType)) baseHealth = 300;
                    
                    // Apply wave health multiplier if available
                    @SuppressWarnings("unchecked")
                    Map<String, Object> waveInfo = (Map<String, Object>) state.get("waveInfo");
                    double healthMultiplier = 1.0;
                    if (waveInfo != null) {
                        Object hMultiplier = waveInfo.get("zombieHealthMultiplier");
                        if (hMultiplier instanceof Number) {
                            healthMultiplier = ((Number) hMultiplier).doubleValue();
                        }
                    }
                    
                    int health = (int) Math.round(baseHealth * healthMultiplier);
                    zombie.put("health", health);
                    zombie.put("maxHealth", health);
                    zombie.put("rotation", Math.random() * Math.PI * 2);
                    zombie.put("playerId", sessionId); // Client that spawned this zombie
                    zombie.put("targetPlayerId", sessionId); // Target player (can be updated later)
                    
                    zombies.add(zombie);
                    
                    // Update zombiesLeft count
                    if (waveInfo != null) {
                        Object zombiesLeft = waveInfo.get("zombiesLeft");
                        if (zombiesLeft instanceof Number) {
                            int left = ((Number) zombiesLeft).intValue() + 1;
                            waveInfo.put("zombiesLeft", left);
                        }
                    }
                    
                    log.info("Spawned zombie {} (type: {}) at ({}, {})", 
                            highestZombieId, zombieType, zombie.get("x"), zombie.get("y"));
                } else {
                    log.warn("spawnZombie data is not a Map: {}", data);
                }
                break;
            }
            case "zombieHit": {
                // Expect: { zombieId, bulletId }
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> zombies = (java.util.List<Map<String, Object>>) state.get("zombies");
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> bullets = (java.util.List<Map<String, Object>>) state.get("bullets");
                
                if (zombies == null || bullets == null) {
                    log.warn("Zombies or bullets list is null in room {}", id);
                    break;
                }
                
                if (data instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> hitData = (Map<String, Object>) data;
                    Object zombieIdObj = hitData.get("zombieId");
                    Object bulletIdObj = hitData.get("bulletId");
                    
                    if (zombieIdObj == null || bulletIdObj == null) {
                        log.warn("zombieHit missing zombieId or bulletId");
                        break;
                    }
                    
                    // Find zombie
                    Map<String, Object> zombie = null;
                    for (Map<String, Object> z : zombies) {
                        if (zombieIdObj.equals(z.get("id"))) {
                            zombie = z;
                            break;
                        }
                    }
                    
                    // Find bullet
                    Map<String, Object> bullet = null;
                    for (Map<String, Object> b : bullets) {
                        if (bulletIdObj.equals(b.get("id"))) {
                            bullet = b;
                            break;
                        }
                    }
                    
                    if (zombie == null || bullet == null) {
                        log.debug("zombieHit: zombie or bullet not found (zombieId: {}, bulletId: {})", zombieIdObj, bulletIdObj);
                        break;
                    }
                    
                    // Get bullet damage
                    Object damageObj = bullet.get("damage");
                    int damage = (damageObj instanceof Number) ? ((Number) damageObj).intValue() : 1;
                    
                    // Decrease bullet pierces
                    Object piercesLeftObj = bullet.get("piercesLeft");
                    int piercesLeft = (piercesLeftObj instanceof Number) ? ((Number) piercesLeftObj).intValue() : 1;
                    piercesLeft--;
                    bullet.put("piercesLeft", piercesLeft);
                    
                    // Remove bullet if no pierces left
                    if (piercesLeft <= 0) {
                        bullets.removeIf(b -> bulletIdObj.equals(b.get("id")));
                    }
                    
                    // Update player damageDealt
                    @SuppressWarnings("unchecked")
                    Map<String, Object> players = (Map<String, Object>) state.get("players");
                    Object bulletPlayerId = bullet.get("playerId");
                    if (bulletPlayerId != null && players != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> player = (Map<String, Object>) players.get(bulletPlayerId.toString());
                        if (player != null) {
                            Object damageDealtObj = player.get("damageDealt");
                            int damageDealt = (damageDealtObj instanceof Number) ? ((Number) damageDealtObj).intValue() : 0;
                            player.put("damageDealt", damageDealt + damage);
                        }
                    }
                    
                    // Apply damage to zombie
                    Object healthObj = zombie.get("health");
                    int zombieHealth = (healthObj instanceof Number) ? ((Number) healthObj).intValue() : 0;
                    zombieHealth -= damage;
                    zombie.put("health", zombieHealth);
                    
                    // Check if zombie is dead
                    if (zombieHealth <= 0) {
                        // Kill zombie - will be handled by killZombie method
                        // Store result for GameSocketHandler to broadcast
                        hitData.put("_zombieKilled", true);
                        hitData.put("_zombieX", zombie.get("x"));
                        hitData.put("_zombieY", zombie.get("y"));
                        hitData.put("_killerPlayerId", bulletPlayerId);
                    } else {
                        // Calculate angle for knockback
                        Object zombieXObj = zombie.get("x");
                        Object zombieYObj = zombie.get("y");
                        Object bulletOriginXObj = bullet.get("originX");
                        Object bulletOriginYObj = bullet.get("originY");
                        
                        double zombieX = (zombieXObj instanceof Number) ? ((Number) zombieXObj).doubleValue() : 0;
                        double zombieY = (zombieYObj instanceof Number) ? ((Number) zombieYObj).doubleValue() : 0;
                        double bulletOriginX = (bulletOriginXObj instanceof Number) ? ((Number) bulletOriginXObj).doubleValue() : 0;
                        double bulletOriginY = (bulletOriginYObj instanceof Number) ? ((Number) bulletOriginYObj).doubleValue() : 0;
                        
                        double angle = Math.atan2(zombieY - bulletOriginY, zombieX - bulletOriginX);
                        Object knockBackObj = bullet.get("knockBack");
                        double knockBack = (knockBackObj instanceof Number) ? ((Number) knockBackObj).doubleValue() : 1.0;
                        
                        // Store for broadcast
                        hitData.put("_angle", angle);
                        hitData.put("_knockBack", knockBack);
                    }
                    
                    log.debug("Zombie {} hit by bullet {} (damage: {}, health remaining: {})", 
                            zombieIdObj, bulletIdObj, damage, zombieHealth);
                } else {
                    log.warn("zombieHit data is not a Map: {}", data);
                }
                break;
            }
            default: {
                log.warn("Unknown message type: {}", type);
            }
        }
    }
    
    /**
     * Kill a zombie
     * Returns true if zombie was found and killed
     */
    public boolean killZombie(Object zombieId, String killerPlayerId) {
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> zombies = (java.util.List<Map<String, Object>>) state.get("zombies");
        if (zombies == null) {
            return false;
        }
        
        // Find and remove zombie
        Map<String, Object> zombieToKill = null;
        for (Map<String, Object> zombie : zombies) {
            if (zombieId.equals(zombie.get("id"))) {
                zombieToKill = zombie;
                break;
            }
        }
        
        if (zombieToKill == null) {
            return false;
        }
        
        zombies.remove(zombieToKill);
        
        // Update player kills
        if (killerPlayerId != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> players = (Map<String, Object>) state.get("players");
            if (players != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> player = (Map<String, Object>) players.get(killerPlayerId);
                if (player != null) {
                    Object killsObj = player.get("kills");
                    int kills = (killsObj instanceof Number) ? ((Number) killsObj).intValue() : 0;
                    player.put("kills", kills + 1);
                }
            }
        }
        
        // Update zombiesLeft count
        @SuppressWarnings("unchecked")
        Map<String, Object> waveInfo = (Map<String, Object>) state.get("waveInfo");
        if (waveInfo != null) {
            Object zombiesLeftObj = waveInfo.get("zombiesLeft");
            if (zombiesLeftObj instanceof Number) {
                int zombiesLeft = ((Number) zombiesLeftObj).intValue();
                if (zombiesLeft > 0) {
                    waveInfo.put("zombiesLeft", zombiesLeft - 1);
                }
            }
        }
        
        log.info("Zombie {} killed by player {}", zombieId, killerPlayerId);
        return true;
    }
    
    /**
     * Calculate zombie spawn type based on wave number
     * Simplified version matching server/src/game/waves.ts logic
     */
    public String calculateZombieSpawnType(int wave) {
        // Wave is 1-indexed, convert to 0-indexed for calculation
        int wave0 = wave - 1;
        
        // Calculate spawn chances based on wave (simplified from config.ts)
        int normalChance = Math.max(0, Math.min(100, 100)); // Always 100
        int babyChance = Math.max(0, Math.min(50, (wave0 + 1) * 6 - 12)); // Start at wave 3
        int mutatedBabyChance = Math.max(0, Math.min(30, (wave0 + 1) * 2 - 8)); // Start at wave 5
        int greenMutantChance = Math.max(0, Math.min(40, (wave0 + 1) * 3 - 3)); // Start at wave 2
        int tankChance = Math.max(0, Math.min(5, (wave0 + 1) * 1 - 3)); // Start at wave 4
        int blueMutantChance = Math.max(0, Math.min(40, (wave0 + 1) * 3 - 15)); // Start at wave 6
        
        int total = normalChance + babyChance + mutatedBabyChance + greenMutantChance + tankChance + blueMutantChance;
        if (total == 0) return "normal";
        
        double random = Math.random() * total;
        random -= normalChance;
        if (random <= 0) return "normal";
        random -= babyChance;
        if (random <= 0) return "baby";
        random -= mutatedBabyChance;
        if (random <= 0) return "mutatedBaby";
        random -= greenMutantChance;
        if (random <= 0) return "greenMutant";
        random -= tankChance;
        if (random <= 0) return "tank";
        return "blueMutant";
    }
    
    
    /**
     * Get current game state
     * This is sent to clients on stateUpdate events
     */
    public Map<String, Object> getState() {
        return state;
    }
}

