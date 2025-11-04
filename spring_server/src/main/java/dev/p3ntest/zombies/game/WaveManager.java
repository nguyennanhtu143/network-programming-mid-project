package dev.p3ntest.zombies.game;

import dev.p3ntest.zombies.game.state.PlayerHealthState;
import dev.p3ntest.zombies.game.state.PlayerState;
import dev.p3ntest.zombies.game.state.WaveInfoState;
import dev.p3ntest.zombies.game.state.ZombieState;
import dev.p3ntest.zombies.websocket.GameRoom;

import java.util.Random;

public class WaveManager {
	private final GameRoom room;
	private int currentWaveNumber = 0;
	private Waves.WaveDef currentWave = Waves.generateWave(0, 1);
	private boolean waveRunning = false;
	private int currentWaveSpawned = 0;
	private boolean finishedSpawning = false;
	private final Random rng = new Random();

	public WaveManager(GameRoom room) {
		this.room = room;
	}

	public void beginNextWave() {
		currentWaveNumber++;
		WaveInfoState info = room.getState().getWaveInfo();
		info.setCurrentWaveNumber(currentWaveNumber);
		info.setActive(true);
		currentWave = Waves.generateWave(currentWaveNumber, room.getPlayerCount());
		waveRunning = true;
		currentWaveSpawned = 0;
		finishedSpawning = false;

		info.setTotalZombies(currentWave.zombies);
		info.setZombiesLeft(currentWave.zombies);
		room.broadcast("/waveStart", java.util.Map.of("wave", currentWaveNumber));
	}

	public boolean isWaveRunning() { return waveRunning; }

	public void spawnTick() {
		if (!waveRunning) return;
		if (currentWaveSpawned >= currentWave.zombies) {
			finishedSpawning = true;
			return;
		}
		currentWaveSpawned++;
		requestSpawnZombie();
	}

	public void checkWaveEnd() {
		if (room.getState().getZombies().isEmpty() && finishedSpawning) {
			waveEnds();
		}
	}

	private void waveEnds() {
		waveRunning = false;
		room.broadcast("/waveEnd", java.util.Map.of("wave", currentWaveNumber));
		room.getState().getWaveInfo().setActive(false);

		// award skill points, revive or heal players
		for (PlayerState p : room.getState().getPlayers().values()) {
			long sp = p.getSkillPoints() + Math.round(currentWaveNumber * GameConfig.SKILL_POINT_MULTIPLIER);
			p.setSkillPoints(sp);
			if (p.getHealthState() == PlayerHealthState.DEAD) {
				p.setHealth(PlayerUtils.getMaxHealth(p));
				p.setHealthState(PlayerHealthState.ALIVE);
			} else if (p.getHealthState() == PlayerHealthState.NOT_SPAWNED) {
				room.sendToPlayer(p.getSessionId(), "/requestSpawn", java.util.Map.of());
			} else {
				p.setHealth(PlayerUtils.getMaxHealth(p));
				p.setWavesSurvived(p.getWavesSurvived() + 1);
			}
		}

		// schedule next wave
		room.getScheduler().schedule(() -> beginNextWave(), java.time.Instant.now().plusMillis(currentWave.postDelayMs));

		// countdown broadcast
		final int[] total = { currentWave.postDelayMs / 1000 };
		var task = new Runnable() {
			@Override public void run() {
				total[0]--;
				room.getState().getWaveInfo().setNextWaveStartsInSec(total[0]);
			}
		};
		room.scheduleAtFixedRate(task, 1000, total[0]);
	}

	public void requestSpawnZombie() {
		if (room.getState().isGameOver()) return;
		if (room.getPlayerCount() == 0) return;
		// pick a random player to own the zombie
		var players = room.getState().getPlayers().values().stream().filter(PlayerState::isConnected).toList();
		if (players.isEmpty()) return;
		PlayerState owner = players.get(rng.nextInt(players.size()));

		var type = Waves.pickZombieType(currentWaveNumber, room.getPlayerCount(), rng).name();
		int health = (int) Math.round(Zombies.INFO.get(Zombies.ZombieType.valueOf(type)).baseHealth() * currentWave.zombieHealthMultiplier);
		ZombieState z = new ZombieState();
		z.setId(room.nextZombieId());
		z.setHealth(health);
		z.setMaxHealth(health);
		z.setX(rng.nextInt(1200) - 200);
		z.setY(rng.nextInt(800) - 100);
		z.setRotation((float) (rng.nextDouble() * Math.PI * 2));
		z.setZombieType(type);
		z.setPlayerId(owner.getSessionId());
		room.getState().getZombies().add(z);
	}

	public void onZombieKilled(long zombieId, String killerSessionId) {
		var it = room.getState().getZombies().iterator();
		while (it.hasNext()) {
			ZombieState z = it.next();
			if (z.getId() == zombieId) {
				it.remove();
				room.getState().getWaveInfo().setZombiesLeft(room.getState().getWaveInfo().getZombiesLeft() - 1);
				room.broadcast("/zombieDead", java.util.Map.of("zombieId", zombieId));
				if (killerSessionId != null) {
					PlayerState p = room.getState().getPlayers().get(killerSessionId);
					if (p != null) p.setKills(p.getKills() + 1);
				}
				break;
			}
		}
		checkWaveEnd();
	}

	public int getCurrentWaveNumber() { return currentWaveNumber; }
	public Waves.WaveDef getCurrentWave() { return currentWave; }
}

