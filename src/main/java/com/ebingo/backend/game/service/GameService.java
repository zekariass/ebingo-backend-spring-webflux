package com.ebingo.backend.game.service;

import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.enums.BingoColumn;
import com.ebingo.backend.game.enums.GameStatus;
import com.ebingo.backend.game.repository.GameRepository;
import com.ebingo.backend.game.service.state.GameStateService;
import com.ebingo.backend.game.service.state.PlayerStateService;
import com.ebingo.backend.game.state.GameState;
import com.ebingo.backend.payment.service.PaymentService;
import com.ebingo.backend.system.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final Map<Long, GameState> activeGames = new ConcurrentHashMap<>();
    private final RedisPublisher publisher;
    private final CardPoolService cardPoolService;
    private final GameRepository gameRepo;
    private final BingoPatternVerifier patternVerifier;
    private final PlayerStateService playerStateService;
    private final GameStateService gameStateService;
    private final PaymentService paymentService;
    private final RedissonReactiveClient redissonReactiveClient;
    private final ReactiveSetOperations<String, Long> setOps;

    private final int drawInterval = 3; // seconds
    private final int minPlayersToStart = 2;

    /**
     * Player joins game and optionally selects a card
     */
//    public Mono<Void> playerJoin(Long roomId, Long gameId, Long userId, Integer capacity, BigDecimal entryFee) {
//        Mono<Boolean> refundPayment = paymentService.processRefund(userId, entryFee);
//
//        return paymentService.processPayment(userId, entryFee)   // 1️⃣ charge first
//                .flatMap(paymentSuccess -> {
//                    if (!paymentSuccess) {
//                        return publisher.publishUserEvent(userId,
//                                Map.of("type", "error", "payload",
//                                        Map.of("message", "Payment failed for user " + userId, "amount", entryFee))).then();
//                    }
//                    return gameStateService.addPlayerToGame(gameId, userId);  // 2️⃣ add player if paid
//                })
//                .then(gameStateService.getGameState(gameId))        // 3️⃣ fetch updated state
//                .flatMap(state -> {
//                    Set<Long> playerIds = state.getJoinedPlayers();
//                    int playersCount = playerIds.size();
//
//                    // 4️⃣ broadcast to room
//                    Mono<Long> broadcastPlayers = publisher.publishEvent(
//                            RedisKeys.roomChannel(roomId),
//                            Map.of(
//                                    "type", "playerJoined",
//                                    "payload", Map.of(
//                                            "joinedPlayers", playerIds,
//                                            "playersCount", playersCount
//                                    )
//                            )
//                    );
//
//                    // 5️⃣ send personal state to user
//                    Mono<Void> sendPlayerState = playerStateService.getPlayerState(gameId, userId)
//                            .flatMap(playerState ->
//                                    publisher.publishUserEvent(userId,
//                                            Map.of(
//                                                    "type", "playerState",
//                                                    "payload", Map.of(
//                                                            "userId", userId,
//                                                            "state", playerState
//                                                    )
//                                            )
//                                    )
//                            )
//                            .then();
//
//                    // 6️⃣ maybe start countdown (after both above are done)
//                    Mono<Void> maybeStartCountdown = Mono.defer(() -> {
//                        if (playerIds.size() >= minPlayersToStart
//                                && !state.isStarted()
//                                && state.getStatus().equals(GameStatus.READY)) {
//                            return startCountdownByGameId(roomId, gameId, capacity, 30);
//                        } else {
//                            return Mono.empty();
//                        }
//                    });
//
//                    // Run broadcast + sendPlayerState in parallel, then countdown
//                    return Mono.when(broadcastPlayers, sendPlayerState)
//                            .then(maybeStartCountdown);
//                })
//                .then()  // return Mono<Void>
//                .onErrorResume(error -> {
//                    log.error("Error in playerJoin for user {} in room {}: {}", userId, roomId, error.getMessage(), error);
//
//                    // Refund only if payment was processed
//                    if (!error.getMessage().contains("Payment failed")) {
//                        return refundPayment
//                                .doOnSuccess(refunded -> log.warn("Refund {} for user {} after join failure",
//                                        refunded ? "succeeded" : "failed", userId))
//                                .then(Mono.empty());
//                    }
//
//                    return Mono.empty();
//                });
//    }


//    public Mono<Void> playerJoin(Long roomId, Long gameId, Long userId, Integer capacity, BigDecimal entryFee) {
//        AtomicBoolean paymentCompleted = new AtomicBoolean(false); // track explicitly
//        Mono<Boolean> refundPayment = paymentService.processRefund(userId, entryFee);
//
//
//        return paymentService.processPayment(userId, entryFee)   // 1️⃣ charge first
//                .flatMap(paymentSuccess -> {
//                    if (!paymentSuccess) {
//                        // No payment → just send error, exit
//                        return publisher.publishUserEvent(userId,
//                                Map.of(
//                                        "type", "error",
//                                        "payload", Map.of(
//                                                "message", "Payment failed for user " + userId,
//                                                "amount", entryFee
//                                        )
//                                )).then(Mono.error(new RuntimeException("PAYMENT_FAILED")));
//                        // short-circuit with error so onErrorResume can distinguish
//                    }
//
//                    log.info("=========================================>>> Payment complete! {}", paymentSuccess);
//
//                    // Payment was successful
//                    paymentCompleted.set(true);
//
//                    // 2️⃣ add player
//                    return gameStateService.addPlayerToGame(gameId, userId);
//                })
//                // 3️⃣ fetch updated state
//                .then(gameStateService.getGameState(roomId)).doOnSuccess(pl -> log.info("============================================>>> PLAYERS: {}", pl))
//                .flatMap(state -> {
//                    Set<Long> playerIds = state.getJoinedPlayers();
//                    int playersCount = playerIds.size();
//
//                    log.info("=======================================>>>: playerIds: {}", playerIds.size());
//
//                    // 4️⃣ broadcast to room
//                    Mono<Long> broadcastPlayers = publisher.publishEvent(
//                            RedisKeys.roomChannel(roomId),
//                            Map.of(
//                                    "type", "playerJoined",
//                                    "payload", Map.of(
//                                            "joinedPlayers", playerIds,
//                                            "playersCount", playersCount
//                                    )
//                            )
//                    );
//
//                    // 5️⃣ send personal state to user
//                    Mono<Void> sendPlayerState = playerStateService.getPlayerState(gameId, userId)
//                            .flatMap(playerState ->
//                                    publisher.publishUserEvent(userId,
//                                            Map.of(
//                                                    "type", "playerState",
//                                                    "payload", Map.of(
//                                                            "userId", userId,
//                                                            "state", playerState
//                                                    )
//                                            )
//                                    )
//                            )
//                            .then();
//
//                    // 6️⃣ maybe start countdown
//                    Mono<Void> maybeStartCountdown = Mono.defer(() -> {
//                        if (playerIds.size() >= minPlayersToStart
//                                && !state.isStarted()
//                                && state.getStatus().equals(GameStatus.READY)) {
//                            return startCountdownByGameId(roomId, gameId, capacity, 30);
//                        } else {
//                            return Mono.empty();
//                        }
//                    });
//
//                    return Mono.when(broadcastPlayers, sendPlayerState)
//                            .then(maybeStartCountdown);
//                })
//                .then()
//                .onErrorResume(error -> {
//                    log.error("Error in playerJoin for user {} in room {}: {}",
//                            userId, roomId, error.getMessage(), error);
//
//                    if (paymentCompleted.get()) {
//                        // refund only if a payment really went through
//                        return refundPayment
//                                .doOnSuccess(refunded -> log.warn("Refund {} for user {} after join failure",
//                                        refunded ? "succeeded" : "failed", userId))
//                                .then();
//                    }
//
//                    // Payment wasn’t taken → just complete
//                    return Mono.empty();
//                });
//    }
    public Mono<Void> playerJoin(Long roomId, Long gameId, Long userId, Integer capacity, BigDecimal entryFee) {
        String playersKey = RedisKeys.gamePlayersKey(gameId);
        AtomicBoolean paymentCompleted = new AtomicBoolean(false);

        return setOps.add(playersKey, userId) // SADD → 1=new user, 0=already joined
                .flatMap(added -> {
                    if (added == 0) {
                        // Already joined → just send existing state
                        log.info("User {} is already joined in game {}", userId, gameId);
                        return sendExistingPlayerState(gameId, userId);
                    }

                    // New join → attempt payment
                    return paymentService.processPayment(userId, entryFee)
                            .flatMap(paymentSuccess -> {
                                if (!paymentSuccess) {
                                    // Payment failed → rollback membership
                                    return setOps.remove(playersKey, userId)
                                            .then(publisher.publishUserEvent(userId,
                                                    Map.of(
                                                            "type", "error",
                                                            "payload", Map.of(
                                                                    "message", "Payment failed for user " + userId,
                                                                    "amount", entryFee
                                                            )
                                                    )))
                                            .then(Mono.error(new RuntimeException("PAYMENT_FAILED")));
                                }

                                paymentCompleted.set(true);
                                log.info("Payment complete for user {} in game {}", userId, gameId);

                                // After payment success → complete join
                                return afterSuccessfulJoin(roomId, gameId, userId, capacity);
                            })
                            .onErrorResume(error -> {
                                // Rollback membership on unexpected errors after payment
                                log.error("Join failed for user {}: {}", userId, error.getMessage(), error);
                                if (paymentCompleted.get()) {
                                    return paymentService.processRefund(userId, entryFee)
                                            .doOnSuccess(refunded ->
                                                    log.warn("Refund {} for user {} after failure",
                                                            refunded ? "succeeded" : "failed", userId))
                                            .then(setOps.remove(playersKey, userId))
                                            .then();
                                }
                                return setOps.remove(playersKey, userId).then();
                            });
                });
    }

    /**
     * Send state to a player who is already joined.
     */
    private Mono<Void> sendExistingPlayerState(Long gameId, Long userId) {
        return playerStateService.getPlayerState(gameId, userId)
                .flatMap(playerState ->
                        publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "playerState",
                                        "payload", Map.of(
                                                "userId", userId,
                                                "state", playerState
                                        )
                                )
                        )
                )
                .then();
    }

    /**
     * Steps to perform after a successful payment and join.
     */
    private Mono<Void> afterSuccessfulJoin(Long roomId, Long gameId, Long userId, Integer capacity) {
        log.info("================================>>>>>: afterSuccessfulJoin for user {} in game {}", userId, gameId);
        return gameStateService.getGameState(roomId)
                .flatMap(state -> {
                    log.info("=====================================>>>: GAME STATE: {}", state);
                    Set<Long> playerIds = state.getJoinedPlayers();
                    int playersCount = playerIds.size();

                    // Broadcast to room
                    Mono<Long> broadcastPlayers = publisher.publishEvent(
                            RedisKeys.roomChannel(roomId),
                            Map.of(
                                    "type", "playerJoined",
                                    "payload", Map.of(
                                            "joinedPlayers", playerIds,
                                            "playersCount", playersCount
                                    )
                            )
                    );

                    // Send state to user
                    Mono<Void> sendPlayerState = playerStateService.getPlayerState(gameId, userId)
                            .flatMap(playerState ->
                                    publisher.publishUserEvent(userId,
                                            Map.of(
                                                    "type", "playerState",
                                                    "payload", Map.of(
                                                            "userId", userId,
                                                            "state", playerState
                                                    )
                                            )
                                    )
                            )
                            .then();

                    // Maybe start countdown
                    Mono<Void> maybeStartCountdown = Mono.defer(() -> {
                        if (playersCount >= minPlayersToStart
                                && !state.isStarted()
                                && state.getStatus().equals(GameStatus.READY)) {
                            return startCountdownByGameId(roomId, gameId, capacity, 10);
                        }
                        return Mono.empty();
                    });

                    return Mono.when(broadcastPlayers, sendPlayerState).then(maybeStartCountdown);
                });
    }


    public Mono<Void> leaveGame(Long roomId, Long gameId, Long userId, BigDecimal entryFee) {


        return gameStateService.getGameState(roomId)
                .flatMap(state -> {

                    log.info("=====================================>>>: GAME: {}", state);
                    if (state == null) {
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "error",
                                        "payload", Map.of(
                                                "errorType", "invalidGame",
                                                "message", "Game already started.",
                                                "userId", userId,
                                                "gameId", gameId
                                        )
                                )).then();
                    }

                    String playersKey = RedisKeys.gamePlayersKey(state.getGameId());

                    boolean gameStarted = state.isStarted();

                    if (gameStarted) {
                        // Already started → personal acknowledgement only
                        log.info("User {} tried to cancel, but game {} already started", userId, gameId);
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "gameLeft",
                                        "payload", Map.of(
                                                "message", "Game already started.",
                                                "userId", userId,
                                                "gameId", gameId
                                        )
                                )).then();
                    }

                    // Game not started → attempt SREM
                    return setOps.remove(playersKey, userId)
                            .flatMap(removed -> {
                                // Explicitly check if user was in the game
                                if (removed == 0) {
                                    log.warn("User {} was not part of game {} when attempting cancel", userId, gameId);
                                    return publisher.publishUserEvent(userId,
                                            Map.of(
                                                    "type", "error",
                                                    "payload", Map.of(
                                                            "errorType", "notInGame",
                                                            "userId", userId,
                                                            "gameId", gameId,
                                                            "success", false,
                                                            "message", "You were not part of the game."
                                                    )
                                            )).then();
                                }

                                log.info("User {} successfully removed from game {}", userId, gameId);

                                // Refund payment
                                Mono<Boolean> refund = paymentService.processRefund(userId, entryFee)
                                        .doOnNext(refunded -> log.info("Refund {} for user {} in game {}",
                                                refunded ? "succeeded" : "failed", userId, gameId));

                                // Personal acknowledgement
                                Mono<Void> personalAck = publisher.publishUserEvent(userId,
                                        Map.of(
                                                "type", "gameLeft",
                                                "payload", Map.of(
                                                        "userId", userId,
                                                        "gameId", gameId,
                                                        "success", true,
                                                        "message", "You have been removed and refunded."
                                                )
                                        )).then();

                                // Broadcast updated players
                                Mono<Long> broadcastPlayers = gameStateService.getGameState(roomId)
                                        .flatMap(updatedState -> {
                                            Set<Long> players = updatedState.getJoinedPlayers();
                                            int playersCount = players.size();

                                            return publisher.publishEvent(
                                                    RedisKeys.roomChannel(roomId),
                                                    Map.of(
                                                            "type", "playerLeft",
                                                            "payload", Map.of(
                                                                    "userId", userId,
                                                                    "joinedPlayers", players,
                                                                    "playersCount", playersCount
                                                            )
                                                    )
                                            );
                                        });

                                return Mono.when(personalAck, refund.then(broadcastPlayers)).then();
                            });
                })
                .onErrorResume(error -> {
                    log.error("Error in playerCancel for user {} in room {}: {}", userId, roomId, error.getMessage(), error);
                    // Always send personal error acknowledgement
                    return publisher.publishUserEvent(userId,
                            Map.of(
                                    "type", "error",
                                    "payload", Map.of(
                                            "errorType", "leaveError",
                                            "userId", userId,
                                            "gameId", gameId,
                                            "success", false,
                                            "message", "Unable to cancel: " + error.getMessage()
                                    )
                            )).then();
                });
    }


    /**
     * Start countdown for a game
     */
    public Mono<Void> startCountdownByGameId(Long roomId, Long gameId, Integer capacity, int countdownSeconds) {

        // Publish countdown start event
        Mono<Long> countdownEvent = publisher.publishEvent(
                RedisKeys.roomChannel(roomId),
                Map.of(
                        "type", "countdown",
                        "payload", Map.of(
                                "roomId", roomId,
                                "gameId", gameId,
                                "duration", countdownSeconds
                        )
                )
        );

        // Start countdown sequence after publishing the event
        return countdownEvent
                .thenMany(Flux.range(0, countdownSeconds)
                        .delayElements(Duration.ofSeconds(1))
                        .doOnNext(sec -> log.debug("Countdown {} / {}", sec + 1, countdownSeconds))
                )
                .then(Mono.defer(() -> startGame(gameId, roomId, capacity))); // Start game after countdown
    }

    /**
     * Start the game
     */
    private Mono<Void> startGame(Long gameId, Long roomId, Integer capacity) {
        return gameStateService.getGameState(roomId)
                .flatMap(state -> {
                    state.setStarted(true);
                    state.setEnded(false);
                    state.setStatus(GameStatus.PLAYING);

                    // Save the updated state first
                    return gameStateService.saveGameStateToRedis(state, roomId)
                            .then(publisher.publishEvent(
                                    RedisKeys.roomChannel(state.getRoomId()),
                                    Map.of(
                                            "type", "gameStarted",
                                            "payload", Map.of("hh", 1) // empty payload
                                    )
                            ))
                            .then(
                                    startNumberDrawingWithLock(state)
                                            .onErrorResume(e -> {
                                                log.error("Number drawing failed", e);
                                                return Mono.empty();
                                            })
                            );
                });
    }

    /**
     * Start number drawing with distributed lock to ensure only one instance handles it
     */
    private Mono<Void> startNumberDrawingWithLock(GameState state) {
        String lockKey = RedisKeys.gameDrawingLockKey(state.getGameId());
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return lock.tryLock(0, 230, TimeUnit.SECONDS) // No wait, 30-second lease
                .flatMap(isLocked -> {
                    if (Boolean.TRUE.equals(isLocked)) {
                        log.info("Instance acquired drawing lock for game {}", state.getGameId());
                        return drawNumbersLoop(state)
                                .then(lock.unlock()
                                        .doOnSuccess(v -> log.info("Released drawing lock for game {}", state.getGameId()))
                                        .onErrorResume(e -> {
                                            log.warn("Failed to unlock drawing lock for game {}", state.getGameId(), e);
                                            return Mono.empty();
                                        })
                                );
                    } else {
                        log.info("Another instance is handling number drawing for game {}", state.getGameId());
                        return Mono.empty();
                    }
                });
    }


    /**
     * Draw numbers until a winner is found or all 75 numbers are called
     */
    private Mono<Void> drawNumbersLoop(GameState state) {
        return Mono.defer(() -> {
            // Get current state from Redis to ensure we have the latest
            return gameStateService.getGameState(state.getRoomId())
                    .flatMap(latestState -> {
                        if (latestState.isEnded() || latestState.getStopNumberDrawing().get()) {
                            log.info("Game {} already ended, stopping number drawing", latestState.getGameId());
                            return Mono.empty();
                        }

                        List<Integer> remaining = new ArrayList<>();
                        for (int i = 1; i <= 75; i++) {
                            if (!latestState.getDrawnNumbers().contains(i)) {
                                remaining.add(i);
                            }
                        }

                        if (remaining.isEmpty()) {
                            log.info("All numbers drawn for game {}", latestState.getGameId());
                            return endGameNoWinner(latestState);
                        }

                        Collections.shuffle(remaining);

                        return Flux.fromIterable(remaining)
                                .delayElements(Duration.ofSeconds(drawInterval))
                                .takeUntil(num -> latestState.isEnded() || latestState.getStopNumberDrawing().get())
                                .concatMap(num -> drawSingleNumber(latestState, num))
                                .doFinally(signal -> {
                                    if (!latestState.isEnded() && !latestState.getStopNumberDrawing().get()) {
                                        log.info("All numbers drawn with no winner for game {}, ending game", latestState.getGameId());
                                        endGameNoWinner(latestState).subscribe();
                                    } else {
                                        log.info("Number drawing stopped for game {} with signal {}", latestState.getGameId(), signal);
                                    }
                                })
                                .then();
                    });
        });
    }

    /**
     * Draw a single number and update state
     */
    private Mono<Void> drawSingleNumber(GameState state, Integer number) {
        return Mono.defer(() -> {
            if (state.isEnded() || state.getStopNumberDrawing().get()) {
                log.info("===========================================>>> Game {} ended during drawing, stopping", state.getGameId());
                return Mono.empty(); // Stop if game ended
            }

            state.getDrawnNumbers().add(number);
            log.info("Drawing number {} for game {}", number, state.getGameId());

            // Save updated state to Redis
            return gameStateService.saveGameStateToRedis(state, state.getRoomId())
                    .then(publisher.publishEvent(
                            RedisKeys.gameChannel(state.getGameId()),
                            Map.of("type", "numberCalled",
                                    "payload", Map.of("number", number))
                    ))
                    .then();
        });
    }

    /**
     * End game when no winner is found (all numbers drawn)
     */
    private Mono<Void> endGameNoWinner(GameState state) {
        state.setEnded(true);
        state.setStatus(GameStatus.COMPLETED);

        return gameStateService.saveGameStateToRedis(state, state.getRoomId())
                .then(publisher.publishEvent(
                        RedisKeys.gameChannel(state.getGameId()),
                        Map.of("type", "gameEnded", "payload", Map.of("reason", "no_winner"))
                ))
                .then(gameStateService.deleteGameState(state.getRoomId()))
                .then();
    }

    /**
     * Claim bingo for any of the player's cards
     */
//    public Mono<Void> claimBingo(Long roomId,
//                                 Long gameId,
//                                 Long userId,
//                                 String username, Map<String, Object> payload) {
//
//        String cardId = (String) payload.get("cardId");
//        List<Integer> markedList = (List<Integer>) payload.get("markedNumbers");
//        Set<Integer> markedNumbers = new HashSet<>(markedList);
//        String pattern = (String) payload.get("pattern");
//
//        return gameStateService.getGameState(roomId)
//                .flatMap(state -> {
//                    // Validate game state
//                    if (!state.isStarted() || state.isEnded()) {
//                        log.warn("User {} attempted to claim bingo but game {} is not active", userId, gameId);
//                        return Mono.error(new RuntimeException("Game not active"));
//                    }
//                    if (state.getDisqualifiedUsers().contains(userId)) {
//                        log.warn("User {} attempted to claim bingo but is disqualified in game {}", userId, gameId);
//                        return Mono.error(new RuntimeException("You are disqualified"));
//                    }
//
//                    // Parallel fetch: all players + player cards
//                    return Mono.zip(
//                                    gameStateService.getAllPlayers(gameId),
//                                    playerStateService.getPlayerCards(gameId, userId)
//                            )
//                            .flatMap(tuple -> {
//                                Set<Long> players = tuple.getT1();
//                                Map<String, CardInfo> playerCards = tuple.getT2();
//
//                                // Validate player
//                                if (!players.contains(userId)) {
//                                    log.info("User {} is not in game {}", userId, gameId);
//                                    return publisher.publishUserEvent(userId,
//                                            Map.of(
//                                                    "type", "error",
//                                                    "payload", Map.of(
//                                                            "message", "User is not in game",
//                                                            "errorType", "USER_NOT_IN_GAME"
//                                                    )
//                                            )
//                                    ).then(); // early exit
//                                }
//
//                                // Validate card existence
//                                CardInfo cardInfo = playerCards.get(cardId);
//                                if (cardInfo == null) {
//                                    log.info("User {} does not have card {} in game {}", userId, cardId, gameId);
//                                    return publisher.publishUserEvent(userId,
//                                            Map.of(
//                                                    "type", "error",
//                                                    "payload", Map.of(
//                                                            "message", "Card not found",
//                                                            "errorType", "CARD_NOT_FOUND"
//                                                    )
//                                            )
//                                    ).then(); // early exit
//                                }
//
//                                // Validate marked numbers
////                                Set<Integer> playerMarkedNumbers = cardInfo.getMarked();
//                                playerStateService.getMarkedNumbers(roomId, userId, cardId)
//                                        .flatMap(playerMarkedNumbers -> {
//                                            if (!markedNumbers.containsAll(playerMarkedNumbers)) {
//                                                log.info("User {} submitted invalid marked numbers for card {} in game {}", userId, cardId, gameId);
//                                                return publisher.publishUserEvent(userId,
//                                                        Map.of(
//                                                                "type", "error",
//                                                                "payload", Map.of(
//                                                                        "message", "Marked numbers mismatch",
//                                                                        "errorType", "MARKED_NUMBERS_MISMATCH"
//                                                                )
//                                                        )
//                                                ).then(); // early exit
//                                            }
//
//                                            return Mono.empty();
//
//                                        });
//
//                                Map<BingoColumn, List<Integer>> card = cardInfo.getNumbers();
//
//                                // Pattern verification (CPU-heavy offloaded)
//                                return Mono.fromCallable(() -> {
//                                            if (pattern != null && !pattern.isBlank()) {
//                                                return patternVerifier.verifyPattern(card, playerMarkedNumbers, pattern);
//                                            } else {
//                                                return patternVerifier.verifyLineOrFourCorners(card, new HashSet<>(playerMarkedNumbers));
//                                            }
//                                        })
//                                        .subscribeOn(Schedulers.boundedElastic())
//                                        .flatMap(isWinner -> {
//                                            if (isWinner) {
//                                                // Winner handling
//                                                log.info("User {} is the winner in game {}", userId, gameId);
//                                                state.getStopNumberDrawing().set(true);
//                                                state.setEnded(true);
//                                                state.setStatus(GameStatus.COMPLETED);
//
//                                                return gameStateService.saveGameStateToRedis(state, state.getRoomId())
//                                                        .then(publisher.publishEvent(
//                                                                RedisKeys.gameChannel(gameId),
//                                                                Map.of(
//                                                                        "type", "bingoWinner",
//                                                                        "payload", Map.of(
//                                                                                "userId", userId,
//                                                                                "userName", username,
//                                                                                "cardId", cardId,
//                                                                                "markedNumbers", playerMarkedNumbers
//                                                                        )
//                                                                )
//                                                        ))
//                                                        .then(gameStateService.deleteGameState(roomId))
//                                                        .doOnError(e -> log.error("Failed to delete game state for room {}", roomId, e))
//                                                        .then(); // final Mono<Void>
//                                            } else {
//                                                // Non-winner handling
//                                                log.info("User {} did not win in game {}, disqualifying", userId, gameId);
//                                                state.getDisqualifiedUsers().add(userId);
//
//                                                return gameStateService.saveGameStateToRedis(state, state.getRoomId())
//                                                        .then(publisher.publishEvent(
//                                                                RedisKeys.gameChannel(gameId),
//                                                                Map.of(
//                                                                        "type", "invalidBingoRequest",
//                                                                        "payload", Map.of(
//                                                                                "userId", userId,
//                                                                                "cardId", cardId,
//                                                                                "markedNumbers", markedNumbers
//                                                                        )
//                                                                )
//                                                        ))
//                                                        .then(); // final Mono<Void>
//                                            }
//                                        });
//                            });
//                })
//                .doOnError(e -> log.error("Error while claiming bingo for user {} in game {}", userId, gameId, e));
//    }
    public Mono<Void> claimBingo(Long roomId,
                                 Long gameId,
                                 Long userId,
                                 String username,
                                 Map<String, Object> payload) {

        // Validate payload
        String cardId = (String) payload.get("cardId");
        @SuppressWarnings("unchecked")
        List<Integer> markedList = (List<Integer>) payload.get("markedNumbers");
        if (cardId == null || markedList == null) {
            return publisher.publishUserEvent(userId,
                    Map.of(
                            "type", "error",
                            "payload", Map.of(
                                    "message", "Invalid claim payload",
                                    "errorType", "INVALID_CLAIM"
                            )
                    )).then();
        }

        Set<Integer> claimedMarkedNumbers = new HashSet<>(markedList);
        String pattern = (String) payload.get("pattern");

        // Top-level: get current game state (or send error if missing)
        return gameStateService.getGameState(roomId)
                .switchIfEmpty(Mono.defer(() ->
                        publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "error",
                                        "payload", Map.of(
                                                "message", "Game not found",
                                                "errorType", "GAME_NOT_FOUND"
                                        )
                                )
                        ).then(Mono.empty())
                ))
                .flatMap(state -> {
                    // Validate game status
                    if (!state.isStarted() || state.isEnded()) {
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "error",
                                        "payload", Map.of(
                                                "message", "Game not active",
                                                "errorType", "GAME_NOT_ACTIVE"
                                        )
                                )).then();
                    }
                    if (state.getDisqualifiedUsers().contains(userId)) {
                        return publisher.publishUserEvent(userId,
                                Map.of(
                                        "type", "error",
                                        "payload", Map.of(
                                                "message", "You are disqualified",
                                                "errorType", "DISQUALIFIED"
                                        )
                                )).then();
                    }

                    // Parallel fetch: players in game + this player's cards
                    return Mono.zip(
                                    gameStateService.getAllPlayers(gameId),
                                    playerStateService.getPlayerCards(gameId, userId)
                            )
                            .flatMap(tuple -> {
                                Set<Long> players = tuple.getT1();
                                Map<String, CardInfo> playerCards = tuple.getT2();

                                // Validate player membership
                                if (!players.contains(userId)) {
                                    return publisher.publishUserEvent(userId,
                                            Map.of(
                                                    "type", "error",
                                                    "payload", Map.of(
                                                            "message", "User is not in game",
                                                            "errorType", "USER_NOT_IN_GAME"
                                                    )
                                            )).then();
                                }

                                CardInfo cardInfo = playerCards.get(cardId);
                                if (cardInfo == null) {
                                    return publisher.publishUserEvent(userId,
                                            Map.of(
                                                    "type", "error",
                                                    "payload", Map.of(
                                                            "message", "Card not found",
                                                            "errorType", "CARD_NOT_FOUND"
                                                    )
                                            )).then();
                                }

                                // Fetch server-tracked marked numbers (reactive) and continue the chain
                                return playerStateService.getMarkedNumbers(roomId, userId, cardId)
                                        .flatMap(serverMarkedNumbers -> {
                                            // Validate submitted numbers vs server-marked numbers
                                            // NOTE: pick your desired semantics. Here we require client's claimed set to contain all server-marked numbers.
                                            if (!claimedMarkedNumbers.containsAll(serverMarkedNumbers)) {
                                                return publisher.publishUserEvent(userId,
                                                        Map.of(
                                                                "type", "error",
                                                                "payload", Map.of(
                                                                        "message", "Marked numbers mismatch",
                                                                        "errorType", "MARKED_NUMBERS_MISMATCH"
                                                                )
                                                        )).then();
                                            }

                                            // Prepare data for pattern verification
                                            Map<BingoColumn, List<Integer>> cardNumbers = cardInfo.getNumbers();
                                            Set<Integer> numbersToVerify = new HashSet<>(serverMarkedNumbers);

                                            // CPU-heavy verification offloaded to boundedElastic
                                            Mono<Boolean> isWinnerMono = Mono.fromCallable(() -> {
                                                        if (pattern != null && !pattern.isBlank()) {
                                                            return patternVerifier.verifyPattern(cardNumbers, numbersToVerify, pattern);
                                                        } else {
                                                            return patternVerifier.verifyLineOrFourCorners(cardNumbers, numbersToVerify);
                                                        }
                                                    })
                                                    .subscribeOn(Schedulers.boundedElastic());

                                            // Evaluate winner and update state atomically-ish (re-check state)
                                            return isWinnerMono.flatMap(isWinner -> {
                                                if (Boolean.TRUE.equals(isWinner)) {
                                                    // Re-fetch latest state to avoid race and check if someone already finished
                                                    return gameStateService.getGameState(roomId)
                                                            .flatMap(latestState -> {
                                                                if (latestState.isEnded()) {
                                                                    // someone else already ended the game; notify the claimer
                                                                    return publisher.publishUserEvent(userId,
                                                                            Map.of(
                                                                                    "type", "bingoResult",
                                                                                    "payload", Map.of(
                                                                                            "success", false,
                                                                                            "message", "Game already finished"
                                                                                    )
                                                                            )).then();
                                                                }

                                                                // Mark game ended
                                                                latestState.getStopNumberDrawing().set(true);
                                                                latestState.setEnded(true);
                                                                latestState.setStatus(GameStatus.COMPLETED);

                                                                return gameStateService.saveGameStateToRedis(latestState, latestState.getRoomId())
                                                                        .then(saveGameToDatabase(latestState)) // async save to DB
                                                                        .then(publisher.publishEvent(
                                                                                RedisKeys.gameChannel(gameId),
                                                                                Map.of(
                                                                                        "type", "bingoWinner",
                                                                                        "payload", Map.of(
                                                                                                "userId", userId,
                                                                                                "userName", username,
                                                                                                "cardId", cardId,
                                                                                                "markedNumbers", serverMarkedNumbers
                                                                                        )
                                                                                )
                                                                        ))
                                                                        .then(gameStateService.deleteGameState(roomId))
                                                                        .then(); // Mono<Void>
                                                            });
                                                } else {
                                                    // Not a winner → disqualify the user
                                                    return gameStateService.getGameState(roomId)
                                                            .flatMap(latestState -> {
                                                                // If already ended, just notify
                                                                if (latestState.isEnded()) {
                                                                    return publisher.publishUserEvent(userId,
                                                                            Map.of(
                                                                                    "type", "bingoResult",
                                                                                    "payload", Map.of(
                                                                                            "success", false,
                                                                                            "message", "Game already finished"
                                                                                    )
                                                                            )).then();
                                                                }

                                                                latestState.getDisqualifiedUsers().add(userId);

                                                                return gameStateService.saveGameStateToRedis(latestState, latestState.getRoomId())
                                                                        .then(publisher.publishEvent(
                                                                                RedisKeys.gameChannel(gameId),
                                                                                Map.of(
                                                                                        "type", "invalidBingoRequest",
                                                                                        "payload", Map.of(
                                                                                                "userId", userId,
                                                                                                "cardId", cardId,
                                                                                                "markedNumbers", new ArrayList<>(claimedMarkedNumbers)
                                                                                        )
                                                                                )
                                                                        )).then();
                                                            });
                                                }
                                            });
                                        });
                            });
                })
                .onErrorResume(e -> {
                    // Log and notify user
                    log.error("Error while claiming bingo for user {} in game {}", userId, gameId, e);
                    return publisher.publishUserEvent(userId,
                            Map.of(
                                    "type", "error",
                                    "payload", Map.of(
                                            "message", "Failed to process bingo claim",
                                            "errorType", "CLAIM_ERROR",
                                            "details", e.getMessage()
                                    )
                            )).then();
                });
    }

    private Mono<Boolean> saveGameToDatabase(GameState latestState) {
        return Mono.just(true);
    }


    public Mono<GameState> getOrInitializeGame(Long roomId, Long userId, Integer capacity) {
        return gameStateService.getOrInitializeGame(roomId, userId, capacity)
                .map(gs -> {
                    log.info("Got or initialized game state: {}", gs.getCurrentCardPool());
                    return gs;
                });
    }

    public Mono<Void> markNumber(Long roomId, Long gameId, Long userId, Map<String, Object> payload) {
        String cardId = (String) payload.get("cardId");
        Integer number = (Integer) payload.get("number");
        if (cardId == null || cardId.isBlank() || !payload.containsKey("number") || number == null || number < 1 || number > 75) {
            return publisher.publishUserEvent(userId, Map.of(
                    "type", "error",
                    "payload", Map.of(
                            "message", "Invalid markNumber payload",
                            "errorType", "CARD_OR_NUMBER_MISSING_OR_INVALID"
                    )
            )).then();
        }
        return playerStateService.addMarkedNumber(gameId, userId, cardId, number)
                .flatMap(updatedCard -> publisher.publishUserEvent(userId, Map.of(
                        "type", "numberMarked",
                        "payload", Map.of(
                                "cardId", cardId,
                                "markedNumbers", updatedCard
                        )
                )))
                .then();
    }

    public Mono<Void> unmarkNumber(Long roomId, Long gameId, Long userId, Map<String, Object> payload) {
        String cardId = (String) payload.get("cardId");
        Integer number = (Integer) payload.get("number");
        if (cardId == null || cardId.isBlank() || !payload.containsKey("number") || number == null || number < 1 || number > 75) {
            return publisher.publishUserEvent(userId, Map.of(
                    "type", "error",
                    "payload", Map.of(
                            "message", "Invalid markNumber payload",
                            "errorType", "CARD_OR_NUMBER_MISSING_OR_INVALID"
                    )
            )).then();
        }
        return playerStateService.removeMarkedNumber(gameId, userId, cardId, number)
                .flatMap(updatedCard -> publisher.publishUserEvent(userId, Map.of(
                        "type", "numberUnmarked",
                        "payload", Map.of(
                                "cardId", cardId,
                                "markedNumbers", updatedCard
                        )
                )))
                .then();
    }
}