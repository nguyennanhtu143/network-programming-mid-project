package dev.p3ntest.zombies.game.state;

import lombok.Data;

@Data
public class PlayerUpgradeState {
    private int fireRate = 0;
    private int damage = 0;
    private int pierce = 0;
    private int health = 0;
    private int speed = 0;
    private int scope = 0;
}


