package dev.p3ntest.zombies.game;

import java.util.Map;

public final class Zombies {
	private Zombies() {}

	public enum ZombieType {
		normal, baby, greenMutant, tank, mutatedBaby, blueMutant
	}

	public record ZombieInfo(int baseHealth, double baseSpeed, int baseAttackDamage, double size, Integer attackDelayTicks) {}

	public static final Map<ZombieType, ZombieInfo> INFO = Map.of(
		ZombieType.normal, new ZombieInfo(100, 1.0, 10, 1.0, null),
		ZombieType.baby, new ZombieInfo(30, 3.0, 10, 0.7, 0),
		ZombieType.mutatedBaby, new ZombieInfo(50, 3.5, 20, 0.7, 0),
		ZombieType.greenMutant, new ZombieInfo(200, 1.0, 20, 1.0, null),
		ZombieType.tank, new ZombieInfo(600, 0.8, 110, 1.9, null),
		ZombieType.blueMutant, new ZombieInfo(300, 1.3, 30, 1.4, null)
	);
}

