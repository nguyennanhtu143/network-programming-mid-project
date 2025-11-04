package dev.p3ntest.zombies.websocket;

import dev.p3ntest.zombies.game.state.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents a game room instance
 * Port of MyRoom.ts from Colyseus
 */
@Slf4j
@Getter
public class GameRoom {
    
    private final String roomId;
    private final GameRoomState state;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler scheduler;
    
    private final int maxClients;
    private final Map<String, String> sessionToUserMap = new ConcurrentHashMap<>();
    
    private long highestZombieId = 0;
    private long highestBulletId = 0;
    
    private ScheduledFuture<?> gameTickTask;
    
    private final String mapId;
    
    public GameRoom(String roomId, String mapId, int maxClients, SimpMessagingTemplate messagingTemplate, TaskScheduler scheduler) {
        this.roomId = roomId;
        this.mapId = mapId;
        this.maxClients = maxClients;
        this.messagingTemplate = messagingTemplate;
        this.scheduler = scheduler;
        
        this.state = new GameRoomState();
        this.state.setMapId(mapId);
    }
    
    public void addPlayer(String sessionId, String username, String userId) {
        if (state.getPlayers().size() >= maxClients) {
            throw new RuntimeException("Room is full");
        }
        
        if (userId != null) {
            sessionToUserMap.put(sessionId, userId);
        }
        
        PlayerState player = new PlayerState();
        player.setSessionId(sessionId);
        player.setName(username != null ? username : "Player " + sessionId.substring(0, 4));
        player.setX((int) (Math.random() * 800));
        player.setY((int) (Math.random() * 600));
        player.setHealth(100);
        player.setPlayerClass("pistol");
        
        state.getPlayers().put(sessionId, player);
        
        broadcastChat(player.getName() + " has joined the game.", "#33ff33");
        log.info("Player {} joined room {}", sessionId, roomId);
    }
    
    public void removePlayer(String sessionId) {
        PlayerState player = state.getPlayers().remove(sessionId);
        if (player != null) {
            broadcastChat(player.getName() + " has left the game.", "#ffff33");
            sessionToUserMap.remove(sessionId);
        }
    }
    
    public void broadcast(String destination, Object payload) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + destination, payload);
    }
    
    public void broadcastChat(String message, String color) {
        Map<String, String> chatMessage = new HashMap<>();
        chatMessage.put("message", message);
        if (color != null) {
            chatMessage.put("color", color);
        }
        broadcast("/chat", chatMessage);
    }
    
    public void broadcastChat(String message) {
        broadcastChat(message, null);
    }
    
    public void sendToPlayer(String sessionId, String destination, Object payload) {
        messagingTemplate.convertAndSend("/queue/player/" + sessionId + destination, payload);
    }
    
    public boolean isEmpty() {
        return state.getPlayers().isEmpty();
    }
    
    public int getPlayerCount() {
        return state.getPlayers().size();
    }
    
    public long nextZombieId() { return ++highestZombieId; }
    public long nextBulletId() { return ++highestBulletId; }

    public TaskScheduler getScheduler() { return scheduler; }

    public void scheduleAtFixedRate(Runnable task, long periodMs, int times) {
        final int[] remaining = { times };
        scheduler.scheduleAtFixedRate(() -> {
            if (remaining[0] <= 0) return;
            task.run();
            remaining[0]--;
        }, Duration.ofMillis(periodMs));
    }

    public void checkGameOverAndFinalize(java.util.function.BiConsumer<String, Integer> saveGameFunc) {
        boolean anyAlive = state.getPlayers().values().stream().anyMatch(p -> p.getHealthState() == PlayerHealthState.ALIVE);
        if (!anyAlive) {
            broadcast("/gameOver", java.util.Map.of());
            state.setGameOver(true);
            int highestWave = (int) state.getWaveInfo().getCurrentWaveNumber();
            saveGameFunc.accept(mapId, highestWave);
        }
    }
    
    // Game tick methods will be implemented in GameRoom logic
    public void startGameTick(Runnable tickLogic) {
        // scheduled by RoomManager
    }
    
    public void stopGameTick() {
        if (gameTickTask != null && !gameTickTask.isCancelled()) {
            gameTickTask.cancel(false);
        }
    }
}

