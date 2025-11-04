package dev.p3ntest.zombies.game.state;

import lombok.Data;

@Data
public class WaveInfoState {
    private long currentWaveNumber = 0;
    private boolean active = false;
    private int nextWaveStartsInSec = 0;
    private long totalZombies = 0;
    private long zombiesLeft = 0;
}


