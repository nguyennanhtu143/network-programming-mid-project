package dev.p3ntest.zombies.game;

import dev.p3ntest.zombies.game.state.PlayerState;

public final class PlayerUtils {
	private PlayerUtils() {}

	public static int getMaxHealth(PlayerState player) {
		int base = GameConfig.PLAYER_STARTING_HEALTH;
		int bonus = player.getUpgrades() != null ? player.getUpgrades().getHealth() * GameConfig.HEALTH_UPGRADE_INCREMENT : 0;
		return base + bonus;
	}

	public static int calculateScore(PlayerState p) {
		// Đơn giản hóa: score = kills*10 + damageDealt/5 + wavesSurvived*50 - deaths*20
		long score = p.getKills() * 10L + p.getDamageDealt() / 5L + p.getWavesSurvived() * 50L - p.getDeaths() * 20L;
		if (score < 0) score = 0;
		return (int) score;
	}
}

