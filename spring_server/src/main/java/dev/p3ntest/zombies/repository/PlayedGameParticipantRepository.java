package dev.p3ntest.zombies.repository;

import dev.p3ntest.zombies.entity.PlayedGameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayedGameParticipantRepository extends JpaRepository<PlayedGameParticipant, String> {
    
    @Query("SELECT p FROM PlayedGameParticipant p LEFT JOIN FETCH p.playedGame pg LEFT JOIN FETCH pg.map LEFT JOIN FETCH pg.participants ORDER BY p.score DESC")
    List<PlayedGameParticipant> findTop10ByOrderByScoreDesc();
}


