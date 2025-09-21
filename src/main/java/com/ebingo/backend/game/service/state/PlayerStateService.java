//package com.ebingo.backend.game.service.state;
//
//import com.ebingo.backend.game.dto.CardInfo;
//import com.ebingo.backend.game.state.PlayerState;
//import com.ebingo.backend.system.redis.RedisKeys;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.ReactiveHashOperations;
//import org.springframework.data.redis.core.ReactiveRedisTemplate;
//import org.springframework.data.redis.core.ReactiveSetOperations;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PlayerStateService {
//
//    private final ReactiveRedisTemplate<String, Object> redis;
//    private final ReactiveHashOperations<String, String, CardInfo> hashOps;
//    private final ReactiveSetOperations<String, String> setOps;
//
//    /**
//     * Save the player's cards in a Redis hash, and marked numbers in Redis sets.
//     */
//    public Mono<Boolean> savePlayerState(Long gameId, PlayerState playerState) {
//        String cardsKey = RedisKeys.playerCardsKey(gameId, playerState.getUserProfileId());
//
//        // Save all cards in one hash
//        Mono<Boolean> saveCards = hashOps.putAll(cardsKey, playerState.getCards())
//                .then(Mono.just(true));
//
//        // Save all marked numbers for each card
//        Flux<Boolean> saveMarkedNumbers = Flux.fromIterable(playerState.getCards().entrySet())
//                .flatMap(entry -> {
//                    String cardId = entry.getKey();
//                    CardInfo card = entry.getValue();
//                    String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, playerState.getUserProfileId(), cardId);
//                    String[] markedStr = card.getMarked().stream()
//                            .map(String::valueOf)
//                            .toArray(String[]::new);
//                    return setOps.add(markedKey, markedStr)
//                            .map(count -> true);
//                });
//
//        return saveCards.thenMany(saveMarkedNumbers).then(Mono.just(true));
//    }
//
//    /**
//     * Retrieve the player's state by combining cards and marked numbers.
//     */
//    public Mono<PlayerState> getPlayerState(Long gameId, Long userId) {
//        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);
//
//        return hashOps.entries(cardsKey)
//                .flatMap(entry -> {
//                    String cardId = entry.getKey();
//                    CardInfo card = entry.getValue();
//
//                    // Fetch marked numbers per card
//                    return getMarkedNumbers(gameId, userId, cardId)
//                            .map(marked -> {
//                                card.setMarked(marked);
//                                return Map.entry(cardId, card);
//                            });
//                })
//                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
//                .map(cards -> {
//                    PlayerState playerState = new PlayerState(userId);
//                    playerState.getCards().putAll(cards);
//                    return playerState;
//                });
//    }
//
//    /**
//     * Save or update a single card and its marked numbers.
//     */
//    public Mono<Boolean> savePlayerCard(Long gameId, Long userId, String cardId, CardInfo bingoCard) {
//        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);
//        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
//
//        Mono<Boolean> saveCard = hashOps.put(cardsKey, cardId, bingoCard);
//        Mono<Boolean> saveMarked = setOps.add(markedKey, bingoCard.getMarked().stream()
//                        .map(String::valueOf)
//                        .toArray(String[]::new))
//                .map(count -> true);
//
//        return Mono.zip(saveCard, saveMarked).thenReturn(true);
//    }
//
//    /**
//     * Fetch all cards for a player, including marked numbers.
//     */
//    public Mono<Map<String, CardInfo>> getPlayerCards(Long gameId, Long userId) {
//        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);
//
//        return hashOps.entries(cardsKey)
//                .flatMap(entry -> {
//                    String cardId = entry.getKey();
//                    CardInfo card = entry.getValue();
//                    return getMarkedNumbers(gameId, userId, cardId)
//                            .map(marked -> {
//                                card.setMarked(marked);
//                                return Map.entry(cardId, card);
//                            });
//                })
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//    }
//
//    /**
//     * Fetch marked numbers for a specific card.
//     */
//    public Mono<Set<Integer>> getMarkedNumbers(Long gameId, Long userId, String cardId) {
//        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
//        return setOps.members(markedKey)
//                .map(Integer::valueOf)
//                .collect(Collectors.toSet());
//    }
//
//
//    public Mono<Boolean> removePlayerCard(Long gameId, Long userId, String cardId) {
//        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);
//        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
//
//        // Remove card from hash and delete the entire marked numbers key
//        Mono<Long> removeCard = hashOps.remove(cardsKey, cardId);
//        Mono<Long> deleteMarked = redis.delete(markedKey);
//
//        return Mono.zip(removeCard, deleteMarked)
//                .map(tuple -> {
//                    Long cardsRemoved = tuple.getT1();
//                    Long markedDeleted = tuple.getT2();
//
//                    log.debug("Removed card {} for user {} in game {}. Cards removed: {}, Marked sets deleted: {}",
//                            cardId, userId, gameId, cardsRemoved, markedDeleted);
//
//                    return cardsRemoved > 0 || markedDeleted > 0;
//                })
//                .defaultIfEmpty(false)
//                .onErrorResume(error -> {
//                    log.error("Failed to remove card {} for user {} in game {}: {}",
//                            cardId, userId, gameId, error.getMessage(), error);
//                    return Mono.just(false);
//                });
//    }
//
//}


package com.ebingo.backend.game.service.state;

import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.state.PlayerState;
import com.ebingo.backend.system.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerStateService {

    private final ReactiveRedisTemplate<String, Object> redis;
    private final ReactiveHashOperations<String, String, CardInfo> hashOps;
    private final ReactiveSetOperations<String, String> setOps;

    private static final Duration PLAYER_STATE_TTL = Duration.ofHours(24);

    /**
     * Save the player's cards in a Redis hash, and marked numbers in Redis sets.
     */
    public Mono<Boolean> savePlayerState(Long gameId, PlayerState playerState) {
        String cardsKey = RedisKeys.playerCardsKey(gameId, playerState.getUserProfileId());

        // Save all cards in one hash
        Mono<Boolean> saveCards = hashOps.putAll(cardsKey, playerState.getCards())
                .then(redis.expire(cardsKey, PLAYER_STATE_TTL))
                .then(Mono.just(true));

        // Save all marked numbers for each card with TTL
        Flux<Boolean> saveMarkedNumbers = Flux.fromIterable(playerState.getCards().entrySet())
                .flatMap(entry -> {
                    String cardId = entry.getKey();
                    CardInfo card = entry.getValue();
                    String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, playerState.getUserProfileId(), cardId);
                    String[] markedStr = card.getMarked().stream()
                            .map(String::valueOf)
                            .toArray(String[]::new);
                    return setOps.add(markedKey, markedStr)
                            .then(redis.expire(markedKey, PLAYER_STATE_TTL))
                            .map(count -> true);
                });

        return saveCards.thenMany(saveMarkedNumbers).then(Mono.just(true));
    }

    /**
     * Retrieve the player's state by combining cards and marked numbers.
     */
    public Mono<PlayerState> getPlayerState(Long gameId, Long userId) {
        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);

        return hashOps.entries(cardsKey)
                .flatMap(entry -> {
                    String cardId = entry.getKey();
                    CardInfo card = entry.getValue();

                    // Fetch marked numbers per card
                    return getMarkedNumbers(gameId, userId, cardId)
                            .map(marked -> {
                                card.setMarked(marked);
                                return Map.entry(cardId, card);
                            });
                })
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(cards -> {
                    PlayerState playerState = new PlayerState(userId);
                    playerState.getCards().putAll(cards);
                    return playerState;
                });
    }

    /**
     * Save or update a single card and its marked numbers.
     */
    public Mono<Boolean> savePlayerCard(Long gameId, Long userId, String cardId, CardInfo bingoCard) {
        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);

        return hashOps.put(cardsKey, cardId, bingoCard)
                .then(redis.expire(cardsKey, PLAYER_STATE_TTL))
                .onErrorResume(e -> {
                    log.error("Failed to save player card {} for user {} in game {}: {}",
                            cardId, userId, gameId, e.getMessage(), e);
                    return Mono.just(false);
                });
    }


    /**
     * Fetch all cards for a player, including marked numbers.
     */
    public Mono<Map<String, CardInfo>> getPlayerCards(Long gameId, Long userId) {
        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);

        return hashOps.entries(cardsKey)
                .flatMap(entry -> {
                    String cardId = entry.getKey();
                    CardInfo card = entry.getValue();
                    return getMarkedNumbers(gameId, userId, cardId)
                            .map(marked -> {
                                card.setMarked(marked);
                                return Map.entry(cardId, card);
                            });
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Fetch marked numbers for a specific card.
     */
    public Mono<Set<Integer>> getMarkedNumbers(Long roomId, Long userId, String cardId) {
        String markedKey = RedisKeys.playerMarkedNumbersKey(roomId, userId, cardId);
        return setOps.members(markedKey)
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * Remove a specific card from player's collection.
     */
    public Mono<Boolean> removePlayerCard(Long gameId, Long userId, String cardId) {
        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);
        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);

        Mono<Long> removeCard = hashOps.remove(cardsKey, cardId);
        Mono<Long> deleteMarked = redis.delete(markedKey);

        return Mono.zip(removeCard, deleteMarked)
                .map(tuple -> {
                    Long cardsRemoved = tuple.getT1();
                    Long markedDeleted = tuple.getT2();

                    log.debug("Removed card {} for user {} in game {}. Cards removed: {}, Marked sets deleted: {}",
                            cardId, userId, gameId, cardsRemoved, markedDeleted);

                    return cardsRemoved > 0 || markedDeleted > 0;
                })
                .defaultIfEmpty(false)
                .onErrorResume(error -> {
                    log.error("Failed to remove card {} for user {} in game {}: {}",
                            cardId, userId, gameId, error.getMessage(), error);
                    return Mono.just(false);
                });
    }

    /**
     * Check if player exists in the game.
     */
    public Mono<Boolean> playerExists(Long gameId, Long userId) {
        String cardsKey = RedisKeys.playerCardsKey(gameId, userId);
        return redis.hasKey(cardsKey);
    }

    public Mono<Set<Integer>> addMarkedNumber(Long gameId, Long userId, String cardId, Integer number) {
        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
        return setOps.add(markedKey, String.valueOf(number))
                .then(redis.expire(markedKey, PLAYER_STATE_TTL))
                .then(setOps.members(markedKey)
                        .map(Integer::valueOf)
                        .collect(Collectors.toSet())
                );
    }

    public Mono<Set<Integer>> removeMarkedNumber(Long gameId, Long userId, String cardId, Integer number) {
        String markedKey = RedisKeys.playerMarkedNumbersKey(gameId, userId, cardId);
        return setOps.remove(markedKey, String.valueOf(number))
                .then(setOps.members(markedKey)
                        .map(Integer::valueOf)
                        .collect(Collectors.toSet())
                );
    }

    /**
     * Get all player IDs in a game.
     */
//    public Mono<Set<Long>> getAllPlayerIds(Long gameId) {
//        String pattern = RedisKeys.playerCardsPattern(gameId);
//        // This would need a keys scan or better pattern matching
//        // For now, delegate to GameStateService which maintains a players set
//        return Mono.empty();
//    }
}