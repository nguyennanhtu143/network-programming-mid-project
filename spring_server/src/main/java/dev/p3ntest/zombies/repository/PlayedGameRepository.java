package dev.p3ntest.zombies.repository;

import dev.p3ntest.zombies.entity.PlayedGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayedGameRepository extends JpaRepository<PlayedGame, String> {
}


