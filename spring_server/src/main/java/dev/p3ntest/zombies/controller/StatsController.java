package dev.p3ntest.zombies.controller;

import dev.p3ntest.zombies.entity.PlayedGameParticipant;
import dev.p3ntest.zombies.repository.PlayedGameParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final PlayedGameParticipantRepository participantRepository;

    @GetMapping("/leaderboard")
    public ResponseEntity<Map<String, List<PlayedGameParticipant>>> getLeaderboard() {
        List<PlayedGameParticipant> leaderboard = participantRepository
            .findTop10ByOrderByScoreDesc();
        return ResponseEntity.ok(Map.of("leaderboard", leaderboard));
    }
}


