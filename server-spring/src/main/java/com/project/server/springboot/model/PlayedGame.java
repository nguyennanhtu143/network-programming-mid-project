package com.project.server.springboot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PlayedGame entity - stores completed game sessions
 */
@Entity
@Table(name = "played_games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayedGame {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id", nullable = false)
    private Map map;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Integer highestWaveSurvived;

    @OneToMany(mappedBy = "playedGame", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayedGameParticipant> participants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}


