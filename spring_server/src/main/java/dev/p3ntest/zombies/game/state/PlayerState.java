package dev.p3ntest.zombies.game.state;

import lombok.Data;

@Data
public class PlayerState {
    private String name = "Unnamed";
    private String sessionId = "";
    
    private int x = 0;
    private int y = 0;
    private float rotation = 0;
    
    private boolean connected = true;
    
    private float velocityX = 0;
    private float velocityY = 0;
    
    private long health = 100; // playerConfig.startingHealth
    private PlayerHealthState healthState = PlayerHealthState.NOT_SPAWNED;
    
    private long skillPoints = 0;
    
    private String playerClass = "pistol";
    
    private PlayerUpgradeState upgrades = new PlayerUpgradeState();
    
    private long kills = 0;
    private long deaths = 0;
    private long damageDealt = 0;
    private long wavesSurvived = 0;
    private long accuracy = 0;
    
    private int currentAnimation = 0;
    
    private boolean finishedLoading = false;
}


