package com.ebingo.backend.game.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("game_card_audit")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameCardAudit {
    @Id
    private Long id;
    private Long gameId;
    private Long userProfileId;
    private Integer cardId;
    private LocalDateTime createdAt;
}

