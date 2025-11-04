package dev.p3ntest.zombies.game.state;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GameRoomState {
    private Map<String, PlayerState> players = new ConcurrentHashMap<>();
    private List<BulletState> bullets = new ArrayList<>();
    private List<ZombieState> zombies = new ArrayList<>();
    
    private long gameTick = 0;
    
    private WaveInfoState waveInfo = new WaveInfoState();
    
    private boolean isGameOver = false;
    
    private String mapId = "";
}


