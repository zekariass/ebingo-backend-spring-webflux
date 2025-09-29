package com.ebingo.backend.game.entity;

import com.ebingo.backend.game.enums.GameStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@Table("game")
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    @Id
    @Column("id")
    private Long id;

    @Column("room_id")
    private Long roomId;

    @Column("joined_players")
    private String joinedPlayersJson;

    @Column("drawn_numbers")
    private String drawnNumberJson;

    @Column("disqualified_users")
    private String disqualifiedUsersJson;

    @Column("all_card_ids")
    private String allCardIdsJson;

    @Column("started")
    private boolean started = false;

    @Column("ended")
    private boolean ended = false;

    @Column("status")
    private GameStatus status;

    @Column("stop_number_drawing")
    private boolean stopNumberDrawing = false;

    @Column("started_at")
    private LocalDateTime startedAt;

    @Column("ended_at")
    private LocalDateTime endedAt;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;


    @Transient
    private List<String> joinedPlayers = new ArrayList<>();

    @Transient
    private List<Integer> drawnNumbers = new ArrayList<>();

    @Transient
    private List<String> disqualifiedUsers = new ArrayList<>();

    @Transient
    private List<String> allCardIds = new ArrayList<>();
}


