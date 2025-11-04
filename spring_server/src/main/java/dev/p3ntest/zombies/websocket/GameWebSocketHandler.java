package dev.p3ntest.zombies.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.p3ntest.zombies.game.PlayerUtils;
import dev.p3ntest.zombies.game.WaveManager;
import dev.p3ntest.zombies.game.state.BulletState;
import dev.p3ntest.zombies.game.state.PlayerHealthState;
import dev.p3ntest.zombies.game.state.PlayerState;
import dev.p3ntest.zombies.game.state.ZombieState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket message handler for game rooms
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketHandler {

    private final RoomManager roomManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MessageMapping("/room/{roomId}/join")
    public void joinRoom(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        String username = (String) payload.get("playerName");
        String userId = (String) payload.get("userId");
        
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            log.warn("Room {} not found for session {}", roomId, sessionId);
            return;
        }
        
        room.addPlayer(sessionId, username, userId);
        
        // Send current state to new player
        room.sendToPlayer(sessionId, "/state", room.getState());
    }

    @MessageMapping("/room/{roomId}/move")
    public void handleMove(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        
        PlayerState player = room.getState().getPlayers().get(sessionId);
        if (player == null) return;
        
        if (message.containsKey("x")) player.setX(((Number) message.get("x")).intValue());
        if (message.containsKey("y")) player.setY(((Number) message.get("y")).intValue());
        if (message.containsKey("rotation")) player.setRotation(((Number) message.get("rotation")).floatValue());
        if (message.containsKey("velocityX")) player.setVelocityX(((Number) message.get("velocityX")).floatValue());
        if (message.containsKey("velocityY")) player.setVelocityY(((Number) message.get("velocityY")).floatValue());
        if (message.containsKey("currentAnimation")) player.setCurrentAnimation(((Number) message.get("currentAnimation")).intValue());
    }

    @MessageMapping("/room/{roomId}/chat")
    public void handleChat(
            @DestinationVariable String roomId,
            @Payload String message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        
        PlayerState player = room.getState().getPlayers().get(sessionId);
        if (player == null) return;
        
        if (message.startsWith("/")) {
            // TODO: command handler
            log.info("Command from {}: {}", player.getName(), message);
            return;
        }
        
        room.broadcastChat(player.getName() + ": " + message);
    }

    @MessageMapping("/room/{roomId}/finishedLoading")
    public void finishedLoading(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        PlayerState player = room.getState().getPlayers().get(sessionId);
        if (player == null) return;
        player.setFinishedLoading(true);
        // spawn player if wave not running
        if (player.getHealthState() == PlayerHealthState.NOT_SPAWNED) {
            room.sendToPlayer(sessionId, "/requestSpawn", java.util.Map.of());
        }
        // simple: auto start wave 1 nếu chưa chạy
        if (room.getState().getWaveInfo().getCurrentWaveNumber() == 0) {
            new WaveManager(room).beginNextWave();
        }
    }

    @MessageMapping("/room/{roomId}/spawnSelf")
    public void spawnSelf(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> body,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        PlayerState player = room.getState().getPlayers().get(sessionId);
        if (player == null) return;
        player.setX(((Number) body.getOrDefault("x", 0)).intValue());
        player.setY(((Number) body.getOrDefault("y", 0)).intValue());
        player.setRotation(((Number) body.getOrDefault("rotation", 0)).floatValue());
        player.setHealthState(PlayerHealthState.ALIVE);
        if (player.getHealth() <= 0) player.setHealth(PlayerUtils.getMaxHealth(player));
    }

    @MessageMapping("/room/{roomId}/shoot")
    public void shoot(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> body,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        BulletState bullet = new BulletState();
        bullet.setId(room.nextBulletId());
        bullet.setPlayerId(sessionId);
        bullet.setOriginX(((Number) body.getOrDefault("originX", 0)).intValue());
        bullet.setOriginY(((Number) body.getOrDefault("originY", 0)).intValue());
        bullet.setRotation(((Number) body.getOrDefault("rotation", 0)).floatValue());
        bullet.setSpeed(((Number) body.getOrDefault("speed", 0)).floatValue());
        bullet.setDamage(((Number) body.getOrDefault("damage", 0)).longValue());
        bullet.setPiercesLeft(((Number) body.getOrDefault("pierces", 0)).intValue());
        Object kb = body.get("knockBack");
        bullet.setKnockBack(kb == null ? 1f : ((Number) kb).floatValue());
        room.getState().getBullets().add(bullet);
    }

    @MessageMapping("/room/{roomId}/destroyBullet")
    public void destroyBullet(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> body) {
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        long id = ((Number) body.get("id")).longValue();
        room.getState().getBullets().removeIf(b -> b.getId() == id);
    }

    @MessageMapping("/room/{roomId}/zombieHit")
    public void zombieHit(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> body,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        Long zombieId = ((Number) body.get("zombieId")).longValue();
        Integer damage = ((Number) body.get("damage")).intValue();
        for (ZombieState z : room.getState().getZombies()) {
            if (z.getId() == zombieId) {
                z.setHealth(z.getHealth() - damage);
                PlayerState p = room.getState().getPlayers().get(sessionId);
                if (p != null) p.setDamageDealt(p.getDamageDealt() + damage);
                if (z.getHealth() <= 0) {
                    new dev.p3ntest.zombies.game.WaveManager(room).onZombieKilled(zombieId, sessionId);
                }
                break;
            }
        }
    }

    @MessageMapping("/room/{roomId}/meleeHitZombie")
    public void meleeHitZombie(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> body,
            SimpMessageHeaderAccessor headerAccessor) {
        zombieHit(roomId, body, headerAccessor);
    }

    @MessageMapping("/room/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) return;
        
        room.removePlayer(sessionId);
        
        // Cleanup empty rooms
        if (room.isEmpty()) {
            roomManager.removeRoom(roomId);
        }
    }
}

