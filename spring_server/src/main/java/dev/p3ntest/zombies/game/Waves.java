package dev.p3ntest.zombies.game;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static dev.p3ntest.zombies.game.Zombies.ZombieType;

public final class Waves {
	private Waves() {}

	public static class WaveDef {
		public final int waveIndex;
		public final int zombies;
		public final int zombieSpawnIntervalMs;
		public final double zombieHealthMultiplier;
		public final double zombieAttackMultiplier;
		public final Map<ZombieType, Double> spawnChances;
		public final int postDelayMs;

		public WaveDef(int waveIndex, int zombies, int spawnIntervalMs, double healthMul, double attackMul,
					 Map<ZombieType, Double> spawnChances, int postDelayMs) {
			this.waveIndex = waveIndex;
			this.zombies = zombies;
			this.zombieSpawnIntervalMs = spawnIntervalMs;
			this.zombieHealthMultiplier = healthMul;
			this.zombieAttackMultiplier = attackMul;
			this.spawnChances = spawnChances;
			this.postDelayMs = postDelayMs;
		}
	}

	public static WaveDef generateWave(int waveNumber, int players) {
		int idx = waveNumber - 1; // 0-indexed
		int base = GameConfig.BASE_ZOMBIES_PER_WAVE + Math.max(0, idx) * 2;
		int zombies = Math.max(1, base + Math.max(players, 1) * GameConfig.ZOMBIES_PER_PLAYER_BONUS);
		int interval = Math.max(150, GameConfig.ZOMBIE_SPAWN_INTERVAL_MS - idx * 10);
		double healthMul = 1.0 + idx * 0.15 + (players - 1) * 0.05;
		double attackMul = 1.0 + idx * 0.08;

		Map<ZombieType, Double> spawn = new EnumMap<>(ZombieType.class);
		spawn.put(ZombieType.normal, clamp(60 - idx * 2, 10, 60) * 1.0);
		spawn.put(ZombieType.baby, clamp(10 + idx, 5, 30) * 1.0);
		spawn.put(ZombieType.mutatedBaby, (double) clamp(5 + idx * 0.8, 0, 20));
		spawn.put(ZombieType.greenMutant, (double) clamp(10 + idx * 0.5, 0, 25));
		spawn.put(ZombieType.tank, (double) clamp(idx * 0.7, 0, 15));
		spawn.put(ZombieType.blueMutant, (double) clamp(5 + idx * 0.7, 0, 20));

		return new WaveDef(idx, zombies, interval, healthMul, attackMul, spawn, GameConfig.POST_WAVE_DELAY_MS);
	}

	public static ZombieType pickZombieType(int waveNumber, int players, Random rng) {
		WaveDef def = generateWave(waveNumber, players);
		double total = def.spawnChances.values().stream().mapToDouble(Double::doubleValue).sum();
		double r = rng.nextDouble() * total;
		for (var e : def.spawnChances.entrySet()) {
			r -= e.getValue();
			if (r <= 0) return e.getKey();
		}
		return ZombieType.normal;
	}

	private static int clamp(double v, double min, double max) {
		return (int) Math.max(min, Math.min(max, v));
	}
}

