package com.ebingo.backend.game.entity;

import com.ebingo.backend.game.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Table("game")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    @Id
    private Long id;
    private Long roomId;

    // Players in the game
    private final Set<Long> joinedPlayers = ConcurrentHashMap.newKeySet();

    // Numbers that have been drawn in the game (order matters)
    private final Set<Integer> drawnNumbers = new LinkedHashSet<>();

    // Users disqualified due to false bingo claims
    private final Set<Long> disqualifiedUsers = ConcurrentHashMap.newKeySet();

    private volatile GameStatus status = GameStatus.READY;

    private volatile boolean started = false;       // only one writer -> fine as volatile
    private volatile boolean ended = false;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
