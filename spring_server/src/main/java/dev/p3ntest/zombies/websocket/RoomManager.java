package dev.p3ntest.zombies.websocket;

import dev.p3ntest.zombies.repository.MapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all game rooms
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomManager {

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;
    private final MapRepository mapRepository;
    
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    public GameRoom createRoom(String mapId, int maxClients) {
        String roomId = generateRoomId();
        
        // If no mapId provided, select a random verified map
        String selectedMapId = mapId;
        if (selectedMapId == null || selectedMapId.isEmpty()) {
            long count = mapRepository.countByVerifiedTrue();
            if (count == 0) {
                throw new RuntimeException("No verified maps available");
            }
            int skip = random.nextInt((int) count);
            selectedMapId = mapRepository.findByVerifiedTrue()
                .stream()
                .skip(skip)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No verified maps found"))
                .getId();
        }
        
        GameRoom room = new GameRoom(roomId, selectedMapId, maxClients, messagingTemplate, taskScheduler);
        rooms.put(roomId, room);
        
        // Start game tick (20 ticks per second)
        taskScheduler.scheduleAtFixedRate(() -> {
            room.getState().setGameTick(room.getState().getGameTick() + 1);
            room.broadcast("/gameTick", room.getState().getGameTick());
        }, Duration.ofMillis(50)); // 1000ms / 20 = 50ms
        
        log.info("Created room {} with map {}", roomId, selectedMapId);
        return room;
    }
    
    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }
    
    public void removeRoom(String roomId) {
        GameRoom room = rooms.remove(roomId);
        if (room != null) {
            room.stopGameTick();
            log.info("Removed room {}", roomId);
        }
    }
    
    public Map<String, GameRoom> getAllRooms() {
        return rooms;
    }
    
    public GameRoom findAvailableRoom() {
        return rooms.values().stream()
            .filter(room -> room.getPlayerCount() < room.getMaxClients())
            .findFirst()
            .orElse(null);
    }
    
    private String generateRoomId() {
        return Long.toHexString(random.nextLong()).substring(0, 5);
    }
    
    public void cleanupEmptyRooms() {
        rooms.entrySet().removeIf(entry -> {
            if (entry.getValue().isEmpty()) {
                entry.getValue().stopGameTick();
                log.info("Cleaned up empty room {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}

