package dev.p3ntest.zombies.controller;

import dev.p3ntest.zombies.websocket.GameRoom;
import dev.p3ntest.zombies.websocket.RoomManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP endpoints for room management
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomManager roomManager;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createRoom(@RequestBody Map<String, Object> request) {
        String mapId = (String) request.get("mapId");
        Integer maxClients = request.containsKey("maxPlayers") 
            ? (Integer) request.get("maxPlayers") 
            : 6;
        
        GameRoom room = roomManager.createRoom(mapId, maxClients);
        
        Map<String, String> response = new HashMap<>();
        response.put("roomId", room.getRoomId());
        response.put("mapId", room.getMapId());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomInfo(@PathVariable String roomId) {
        GameRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> info = new HashMap<>();
        info.put("roomId", room.getRoomId());
        info.put("mapId", room.getMapId());
        info.put("playerCount", room.getPlayerCount());
        info.put("maxClients", room.getMaxClients());
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, String>> findAvailableRoom() {
        GameRoom room = roomManager.findAvailableRoom();
        
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("roomId", room.getRoomId());
        response.put("mapId", room.getMapId());
        
        return ResponseEntity.ok(response);
    }
}


