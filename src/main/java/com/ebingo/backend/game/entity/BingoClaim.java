package com.ebingo.backend.game.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Table("bingo_claim")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BingoClaim {
    @Id
    private Long id;
    private Long gameId;
    private Long userProfileId;
    private Long gameEntryId; // may be null if no mapping to DB entry
    private Boolean isWinner;
    private LocalDateTime claimedAt;
    private LocalDateTime verifiedAt;
    private List<Integer> claimedMarkedNumbers;
}
