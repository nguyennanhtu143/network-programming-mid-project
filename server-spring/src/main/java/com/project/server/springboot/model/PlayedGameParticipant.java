package com.project.server.springboot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PlayedGameParticipant entity - stores player statistics for a game session
 */
@Entity
@Table(name = "played_game_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayedGameParticipant {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "played_game_id", nullable = false)
    private PlayedGame playedGame;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String username = "Anonymous";

    @Column(nullable = false)
    private Integer kills = 0;

    @Column(nullable = false)
    private Integer deaths = 0;

    @Column(nullable = false)
    private Double accuracy = 0.0;

    @Column(nullable = false)
    private Integer wavesSurvived = 0;

    @Column(nullable = false)
    private Integer damageDealt = 0;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}


