package com.ebingo.backend.game.state;

import com.ebingo.backend.game.dto.CardInfo;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a player in the game, holding up to 2 cards.
 * Each card has its own state (numbers, marked numbers).
 */
@Data
public class PlayerState {

    private Long userProfileId;

    // cardId -> BingoCard
    private Map<String, CardInfo> cards = new ConcurrentHashMap<>();

    public PlayerState(Long userProfileId) {
        this.userProfileId = userProfileId;
    }
}
