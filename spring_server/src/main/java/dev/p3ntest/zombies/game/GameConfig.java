package dev.p3ntest.zombies.game;

public final class GameConfig {
	private GameConfig() {}

	public static final int TICK_RATE = 20; // Hz

	// Player config
	public static final int PLAYER_STARTING_HEALTH = 100;
	public static final int HEALTH_UPGRADE_INCREMENT = 20;

	// Skill points
	public static final double SKILL_POINT_MULTIPLIER = 1.0; // per wave

	// Waves
	public static final int BASE_ZOMBIES_PER_WAVE = 10;
	public static final int ZOMBIES_PER_PLAYER_BONUS = 3;
	public static final int POST_WAVE_DELAY_MS = 8000;
	public static final int ZOMBIE_SPAWN_INTERVAL_MS = 400; // during wave
}

