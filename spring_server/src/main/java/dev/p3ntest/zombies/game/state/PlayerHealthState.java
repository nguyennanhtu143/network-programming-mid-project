package dev.p3ntest.zombies.game.state;

public enum PlayerHealthState {
    ALIVE(0),
    DEAD(1),
    NOT_SPAWNED(2);

    private final int value;

    PlayerHealthState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PlayerHealthState fromValue(int value) {
        for (PlayerHealthState state : values()) {
            if (state.value == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown PlayerHealthState: " + value);
    }
}


