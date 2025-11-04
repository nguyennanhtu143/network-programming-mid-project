package dev.p3ntest.zombies.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class GameServerHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Add custom health checks here
        // For now, always return UP
        return Health.up()
            .withDetail("gameServer", "operational")
            .build();
    }
}


