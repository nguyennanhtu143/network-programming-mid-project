package dev.p3ntest.zombies.game.state;

import lombok.Data;

@Data
public class ZombieState {
    private long id = 0;
    private int x = 0;
    private int y = 0;
    private float rotation = 0;
    private String playerId = "";
    private long health = 100;
    private long maxHealth = 100;
    private String targetPlayerId = "";
    
    private long lastAttackTick = 0;
    private long attackCoolDownTicks = 20;
    
    private String zombieType = "normal";
}


