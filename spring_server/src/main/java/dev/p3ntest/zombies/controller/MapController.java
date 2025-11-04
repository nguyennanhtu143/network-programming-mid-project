package dev.p3ntest.zombies.controller;

import com.fasterxml.jackson.databind.JsonNode;
import dev.p3ntest.zombies.dto.MapInfo;
import dev.p3ntest.zombies.entity.Map;
import dev.p3ntest.zombies.entity.User;
import dev.p3ntest.zombies.service.MapService;
import dev.p3ntest.zombies.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final UserService userService;

    @GetMapping("/load/{mapId}")
    public ResponseEntity<MapInfo> loadMap(@PathVariable String mapId) {
        return ResponseEntity.ok(mapService.loadMap(mapId));
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyMap(
            @RequestBody java.util.Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        String mapId = (String) request.get("mapId");
        Boolean verify = (Boolean) request.get("verify");
        return ResponseEntity.ok(mapService.verifyMap(mapId, verify, user));
    }

    @GetMapping("/my-maps")
    public ResponseEntity<List<MapInfo>> getMyMaps(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(mapService.getMyMaps(user.getId()));
    }

    @GetMapping("/my-maps/{mapId}")
    public ResponseEntity<MapInfo> getMyMapOne(
            @PathVariable String mapId,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        return ResponseEntity.ok(mapService.getMyMapOne(mapId, user.getId()));
    }

    @PostMapping("/publish")
    public ResponseEntity<Map> setPublishMap(
            @RequestBody java.util.Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        String mapId = (String) request.get("mapId");
        Boolean publish = (Boolean) request.get("publish");
        return ResponseEntity.ok(mapService.setPublishMap(mapId, publish, user.getId()));
    }

    @PutMapping("/overwrite")
    public ResponseEntity<Map> overwriteMap(
            @RequestBody java.util.Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        String mapId = (String) request.get("mapId");
        JsonNode level = (JsonNode) request.get("level");
        return ResponseEntity.ok(mapService.overwriteMap(mapId, level, user.getId()));
    }

    @DeleteMapping("/{mapId}")
    public ResponseEntity<Void> deleteMap(
            @PathVariable String mapId,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        mapService.deleteMap(mapId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/save-new")
    public ResponseEntity<Map> saveNewMap(
            @RequestBody java.util.Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {
        User user = userService.getUserFromJwt(jwt);
        String name = (String) request.get("name");
        JsonNode level = (JsonNode) request.get("level");
        return ResponseEntity.ok(mapService.saveNewMap(name, level, user.getId()));
    }

    @GetMapping("/to-play")
    public ResponseEntity<java.util.Map<String, Object>> getMapsToPlay(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.ok(mapService.getMapsToPlay(userId));
    }
}


