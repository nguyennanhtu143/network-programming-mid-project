package dev.p3ntest.zombies.game.state;

import lombok.Data;

@Data
public class BulletState {
    private long id = 0;
    private String playerId = "";
    private int originX = 0;
    private int originY = 0;
    private float rotation = 0;
    private float speed = 0;
    private long damage = 0;
    private int piercesLeft = 0;
    private float knockBack = 1;
}


