package com.ebingo.backend.game.state;

import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.enums.GameStatus;
import lombok.Data;
import lombok.ToString;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents the state of a single Bingo game in a room.
 */
@Data
@ToString
public class GameState {

    private Long gameId;
    private Long roomId;

    // Players in the game
    private final Set<Long> joinedPlayers = ConcurrentHashMap.newKeySet();

    // Numbers that have been drawn in the game (order matters)
    private final Set<Integer> drawnNumbers = new LinkedHashSet<>();

    // Users disqualified due to false bingo claims
    private final Set<Long> disqualifiedUsers = ConcurrentHashMap.newKeySet();

    // Card Pool
    private final List<CardInfo> currentCardPool = new ArrayList<>();
    private final List<CardInfo> nextCardPool = new ArrayList<>();

    // Game status flags
    private volatile boolean started = false;       // only one writer -> fine as volatile
    private volatile boolean ended = false;
    private volatile GameStatus status = GameStatus.READY;

    private AtomicBoolean stopNumberDrawing = new AtomicBoolean(false);


    // Start game
    public void startGame() {
        started = true;
    }

    // End game safely (only once)
    public void endGame() {
        ended = true;
    }

    // ---------------------------
    // Reactive-friendly methods
    // ---------------------------


    public void setJoinedPlayers(Set<Long> userIds) {
        joinedPlayers.clear();
//        return Mono.fromSupplier(() -> joinedPlayers.addAll(userIds));
        joinedPlayers.addAll(userIds);
    }

    /**
     * Add a drawn number.
     */
    public void addDrawnNumber(Integer number) {
        drawnNumbers.add(number);
    }

    public void setDrawnNumber(LinkedHashSet<Integer> nums) {
        drawnNumbers.clear();
        drawnNumbers.addAll(nums);
    }

    /**
     * Disqualify a player.
     */
    public void disqualifyPlayer(Long userId) {
        disqualifiedUsers.add((userId));
    }

    public void setDisqualifiedPlayers(Set<Long> userIds) {
        disqualifiedUsers.clear();
        disqualifiedUsers.addAll(userIds);
    }

    /**
     * Set the card pool (replaces existing pool).
     */
    public Mono<Void> setCurrentCardPool(List<CardInfo> newPool) {
        return Mono.fromRunnable(() -> {
            currentCardPool.clear();
            currentCardPool.addAll(newPool);
        });
    }

//    public Mono<Void> setNextCardPool(List<CardInfo> newPool) {
//        return Mono.fromRunnable(() -> {
//            nextCardPool.clear();
//            nextCardPool.addAll(newPool);
//        });
//    }

}
