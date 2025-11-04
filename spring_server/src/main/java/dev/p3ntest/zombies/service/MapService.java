package dev.p3ntest.zombies.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.p3ntest.zombies.dto.MapInfo;
import dev.p3ntest.zombies.entity.Map;
import dev.p3ntest.zombies.entity.User;
import dev.p3ntest.zombies.repository.MapRepository;
import dev.p3ntest.zombies.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapService {

    private final MapRepository mapRepository;
    private final UserRepository userRepository;

    public MapInfo loadMap(String mapId) {
        Map map = mapRepository.findById(mapId)
            .orElseThrow(() -> new RuntimeException("Map not found"));
        return toMapInfo(map);
    }

    public List<MapInfo> getMyMaps(String userId) {
        return mapRepository.findByAuthorId(userId).stream()
            .map(this::toMapInfo)
            .collect(Collectors.toList());
    }

    public MapInfo getMyMapOne(String mapId, String userId) {
        Map map = mapRepository.findById(mapId)
            .filter(m -> m.getAuthor().getId().equals(userId))
            .orElseThrow(() -> new RuntimeException("Map not found or unauthorized"));
        return toMapInfo(map);
    }

    @Transactional
    public String verifyMap(String mapId, boolean verify, User user) {
        if (!user.getScopePermissions().contains("verify:maps")) {
            return "Unauthorized";
        }
        
        Map map = mapRepository.findById(mapId)
            .orElseThrow(() -> new RuntimeException("Map not found"));
        map.setVerified(verify);
        mapRepository.save(map);
        
        return "Map " + map.getName() + " has been " + (verify ? "verified" : "unverified");
    }

    @Transactional
    public Map setPublishMap(String mapId, boolean publish, String userId) {
        Map map = mapRepository.findById(mapId)
            .filter(m -> m.getAuthor().getId().equals(userId))
            .orElseThrow(() -> new RuntimeException("Map not found or unauthorized"));
        map.setPublished(publish);
        return mapRepository.save(map);
    }

    @Transactional
    public Map overwriteMap(String mapId, JsonNode level, String userId) {
        Map map = mapRepository.findById(mapId)
            .filter(m -> m.getAuthor().getId().equals(userId))
            .orElseThrow(() -> new RuntimeException("Map not found or unauthorized"));
        map.setLevel(level);
        return mapRepository.save(map);
    }

    @Transactional
    public void deleteMap(String mapId, String userId) {
        Map map = mapRepository.findById(mapId)
            .filter(m -> m.getAuthor().getId().equals(userId))
            .orElseThrow(() -> new RuntimeException("Map not found or unauthorized"));
        mapRepository.delete(map);
    }

    @Transactional
    public Map saveNewMap(String name, JsonNode level, String userId) {
        User author = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map map = new Map();
        map.setId(UUID.randomUUID().toString());
        map.setName(name);
        map.setLevel(level);
        map.setAuthor(author);
        
        return mapRepository.save(map);
    }

    public java.util.Map<String, Object> getMapsToPlay(String userId) {
        List<MapInfo> verifiedMaps = mapRepository.findByVerifiedTrue().stream()
            .map(this::toMapInfo)
            .collect(Collectors.toList());
        
        List<MapInfo> myMaps = userId != null 
            ? mapRepository.findByAuthorIdAndVerifiedFalse(userId).stream()
                .map(this::toMapInfo)
                .collect(Collectors.toList())
            : null;
        
        List<MapInfo> communityMaps = userId != null
            ? mapRepository.findPublishedCommunityMaps(userId).stream()
                .limit(20)
                .map(this::toMapInfo)
                .collect(Collectors.toList())
            : mapRepository.findByPublishedTrueOrderByCreatedAtDesc().stream()
                .limit(20)
                .map(this::toMapInfo)
                .collect(Collectors.toList());
        
        return java.util.Map.of(
            "verifiedMaps", verifiedMaps,
            "myMaps", myMaps != null ? myMaps : List.of(),
            "communityMaps", communityMaps
        );
    }

    private MapInfo toMapInfo(Map map) {
        return new MapInfo(
            map.getId(),
            map.getLevel(),
            map.getName(),
            map.getVerified(),
            map.getPublished()
        );
    }
}


