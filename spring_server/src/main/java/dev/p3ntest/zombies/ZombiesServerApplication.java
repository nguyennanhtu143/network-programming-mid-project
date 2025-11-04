package dev.p3ntest.zombies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZombiesServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZombiesServerApplication.class, args);
    }
}


