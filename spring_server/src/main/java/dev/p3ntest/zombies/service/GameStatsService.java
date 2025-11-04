package dev.p3ntest.zombies.service;

import dev.p3ntest.zombies.entity.PlayedGame;
import dev.p3ntest.zombies.entity.PlayedGameParticipant;
import dev.p3ntest.zombies.entity.User;
import dev.p3ntest.zombies.game.PlayerUtils;
import dev.p3ntest.zombies.game.state.PlayerState;
import dev.p3ntest.zombies.repository.MapRepository;
import dev.p3ntest.zombies.repository.PlayedGameParticipantRepository;
import dev.p3ntest.zombies.repository.PlayedGameRepository;
import dev.p3ntest.zombies.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameStatsService {

	private final PlayedGameRepository playedGameRepository;
	private final PlayedGameParticipantRepository participantRepository;
	private final MapRepository mapRepository;
	private final UserRepository userRepository;

	@Transactional
	public void saveGame(String mapId, int highestWave, Iterable<PlayerState> players, java.util.Map<String, String> sessionToUserMap) {
		PlayedGame pg = new PlayedGame();
		pg.setId(UUID.randomUUID().toString());
		pg.setMap(mapRepository.findById(mapId).orElseThrow());
		pg.setHighestWaveSurvived(highestWave);
		playedGameRepository.save(pg);

		List<PlayedGameParticipant> parts = new ArrayList<>();
		for (PlayerState p : players) {
			PlayedGameParticipant part = new PlayedGameParticipant();
			part.setId(UUID.randomUUID().toString());
			part.setPlayedGame(pg);
			String userId = sessionToUserMap.get(p.getSessionId());
			if (userId != null) {
				User u = userRepository.findById(userId).orElse(null);
				part.setUser(u);
			}
			part.setUsername(p.getName());
			part.setKills((int) p.getKills());
			part.setDeaths((int) p.getDeaths());
			part.setAccuracy((double) p.getAccuracy());
			part.setWavesSurvived((int) p.getWavesSurvived());
			part.setDamageDealt((int) p.getDamageDealt());
			part.setScore(PlayerUtils.calculateScore(p));
			parts.add(part);
		}
		participantRepository.saveAll(parts);
	}
}

