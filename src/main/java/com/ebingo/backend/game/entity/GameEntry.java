package com.ebingo.backend.game.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

@Table("game_entries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameEntry {

    @Id
    private Long id;

    @Column("user_profile_id")
    private Long userProfileId;

    @Column("game_id")
    private Long gameId;

    // Example card structure: { "cardId": 5, "grid": [[..],[..],..] }
    @Column("card")
    private Map<String, Object> card;

    @Column("joined_at")
    private LocalDateTime joinedAt;
}
