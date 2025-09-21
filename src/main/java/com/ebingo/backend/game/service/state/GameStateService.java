package com.ebingo.backend.game.service.state;

import com.ebingo.backend.game.dto.CardInfo;
import com.ebingo.backend.game.enums.GameStatus;
import com.ebingo.backend.game.service.CardPoolService;
import com.ebingo.backend.game.service.RedisPublisher;
import com.ebingo.backend.game.state.GameState;
import com.ebingo.backend.game.state.PlayerState;
import com.ebingo.backend.system.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameStateService {

    private final ReactiveRedisTemplate<String, Object> redis;
    private final ReactiveHashOperations<String, String, Object> hashOps;
    private final ReactiveSetOperations<String, Object> setOps;
    private final CardPoolService cardPoolService;
    private final PlayerStateService playerStateService; // Add this dependency
    private final RedissonReactiveClient redissonReactiveClient;
    private final RedisPublisher publisher;
//    private final RoomStateService roomStateService;

    private static final Duration GAME_STATE_TTL = Duration.ofHours(24);

    // ----------------------------
    // Initialize Game
    // ----------------------------
    public Mono<GameState> getOrInitializeGame(Long roomId, Long userId, int capacity) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        return redis.hasKey(gameKey)
                .flatMap(exists -> {
                    if (exists) {
                        log.info("================================>>>> Game already exists for room: {}", roomId);
                        return getGameState(roomId);
                    } else {
                        log.info("================================>>>> Game does not exist for room: {}", roomId);
                        return initializeGameWithLock(roomId, userId, capacity);
                    }
                });
    }

    public Mono<GameState> initializeGameWithLock(Long roomId, Long userId, int capacity) {
        return initializeGameWithLockWithRetry(roomId, userId, capacity, 0);
    }

    private Mono<GameState> initializeGameWithLockWithRetry(Long roomId, Long userId, int capacity, int retryCount) {
        log.info("================================>>>> Attempting to initialize game for room: {}", roomId);
        final int MAX_RETRIES = 3;

        if (retryCount >= MAX_RETRIES) {
            publisher.publishUserEvent(userId,
                    Map.of(
                            "type", "error",
                            "payload", Map.of(
                                    "message", "Maximum retry to initialize the game."
                            )
                    ));
            return Mono.empty();
        }

        String lockKey = RedisKeys.gameInitLockKey(roomId);
        RLockReactive lock = redissonReactiveClient.getLock(lockKey);

        return lock.tryLock(0, 10, TimeUnit.SECONDS) // don't wait, lease 10s
                .flatMap(isLocked -> {
                    if (Boolean.TRUE.equals(isLocked)) {
                        log.info("=================================>>> Instance acquired game initialization lock for room: {}", roomId);
                        return Mono.usingWhen(
                                Mono.just(lock),
                                l -> initializeGame(roomId, capacity)
                                        .flatMap(this::saveGameStateToDb)
                                        .flatMap(gsFromDb ->
                                                cardPoolService.getCurrentPool(roomId)
                                                        .flatMap(pool ->
                                                                gsFromDb.setCurrentCardPool(pool)  // this returns Mono<Void>
                                                                        .then(saveGameStateToRedis(gsFromDb, roomId)) // chain save after setter completes
                                                                        .thenReturn(gsFromDb) // finally return updated GameState
                                                        )
                                        ),
                                l -> l.unlock().onErrorResume(e -> Mono.empty())
                        );
                    } else {
                        return Mono.delay(Duration.ofSeconds(1)) // backoff
                                .then(checkAndGetExistingGameState(roomId, capacity))
                                .switchIfEmpty(Mono.defer(() ->
                                        initializeGameWithLockWithRetry(roomId, userId, capacity, retryCount + 1)
                                ));
                    }
                });
    }


    /**
     * Checks Redis for an existing game state.
     * If found, it returns the state; otherwise, it throws an error or returns an empty Mono.
     */
    private Mono<GameState> checkAndGetExistingGameState(Long roomId, int capacity) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        // Check if the key exists in Redis
        return redis.hasKey(gameKey)
                .flatMap(hasKey -> {
                    if (Boolean.TRUE.equals(hasKey)) {
                        return getGameState(roomId); // gameId and capacity can be fetched or passed as needed
                    } else {
                        // The game has not been initialized yet, and we couldn't get the lock.
                        // This is an edge case that might require retrying or an error response.
                        log.error("Game {} is not yet initialized. Please retry.", roomId);
                        return Mono.empty();
                    }
                });
    }


    public Mono<GameState> initializeGame(Long roomId, Integer capacity) {
        log.info("================================>>>> Creating game for room: {}", roomId);
        // 1. Initialize GameState object
        GameState gameState = new GameState();
        gameState.setRoomId(roomId);
        gameState.setStarted(false);
        gameState.setEnded(false);
        gameState.setStatus(GameStatus.READY);
        gameState.getStopNumberDrawing().set(false);


        gameState.setDrawnNumber(new LinkedHashSet<>());
        gameState.setDisqualifiedPlayers(Set.of());
        gameState.setCurrentCardPool(List.of());
//        gameState.setNextCardPool(List.of());
        gameState.setJoinedPlayers(Set.of());

        return Mono.when(
                        cardPoolService.generateAndStoreCurrentPool(roomId, capacity)
//                        cardPoolService.generateAndStoreNextPool(roomId, capacity)
                )
                .then(Mono.just(gameState));

    }

    // Placeholder for actual DB save logic
    private Mono<GameState> saveGameStateToDb(GameState gameState) {
        // Simulate DB save and ID generation
        return Mono.just(gameState)
                .map(gs -> {
                    gs.setGameId(ThreadLocalRandom.current().nextLong(1, 10001)); // Simulate setting a generated ID
                    return gs;
                });
    }

    public Mono<Boolean> saveGameStateToRedis(GameState gameState, Long roomId) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        Map<String, Object> gameData = Map.of(
                "gameId", String.valueOf(gameState.getGameId()),
                "roomId", String.valueOf(gameState.getRoomId()),
                "started", String.valueOf(gameState.isStarted()),
                "ended", String.valueOf(gameState.isEnded()),
                "status", gameState.getStatus().name(),
                "stopNumberDrawing", gameState.getStopNumberDrawing().toString()
//                "winnerId", gameState.getWinnerId() != null ? String.valueOf(gameState.getWinnerId()) : ""
        );

        // Save game metadata and then set TTL
        return hashOps.putAll(gameKey, gameData)
                .then(redis.expire(gameKey, GAME_STATE_TTL))
                .thenReturn(true)
                .onErrorResume(e -> {
                    System.err.println("Failed to save game state: " + e.getMessage());
                    return Mono.just(false);
                });
    }

    // ----------------------------
    // Add Drawn Number (delta)
    // ----------------------------


    public Mono<LinkedHashSet<Integer>> addOrInitDrawnNumber(Long gameId, int number) {
        String drawnKey = RedisKeys.gameDrawnNumbersKey(gameId);

        return redis.hasKey(drawnKey)
                .flatMap(exists -> {
                    Mono<Boolean> addNumber = setOps.add(drawnKey, String.valueOf(number))
                            .thenReturn(true)
                            .onErrorResume(e -> {
                                log.error("Failed to add drawn number {} for game {}: {}", number, gameId, e.getMessage(), e);
                                return Mono.just(false);
                            });

                    if (!exists) {
                        // First time: add + set TTL
                        return addNumber.then(redis.expire(drawnKey, GAME_STATE_TTL))
                                .thenMany(setOps.members(drawnKey))
                                .map(obj -> Integer.valueOf((String) obj))
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                    } else {
                        // Just add
                        return addNumber
                                .thenMany(setOps.members(drawnKey))
                                .map(obj -> Integer.valueOf((String) obj))
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                    }
                });

    }

    public Mono<LinkedHashSet<Integer>> getDrawnNumbers(Long gameId) {
        String drawnKey = RedisKeys.gameDrawnNumbersKey(gameId);
        return setOps.members(drawnKey)
                .map(obj -> Integer.valueOf((String) obj))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .onErrorResume(e -> Mono.just(new LinkedHashSet<>()));
    }

    // ----------------------------
    // PlayerState Management - DELEGATED to PlayerStateService
    // ----------------------------
    public Mono<Boolean> savePlayerState(Long gameId, PlayerState playerState) {
        // Delegate to PlayerStateService for actual storage
        return playerStateService.savePlayerState(gameId, playerState)
                .then(setOps.add(RedisKeys.gamePlayersKey(gameId), playerState.getUserProfileId().toString()))
                .thenReturn(true)
                .onErrorResume(e -> {
                    log.error("Failed to save player {} in game {}: {}", playerState.getUserProfileId(), gameId, e.getMessage(), e);
                    return Mono.just(false);
                });
    }

    public Mono<PlayerState> getPlayerState(Long gameId, Long userId) {
        // Delegate to PlayerStateService
        return playerStateService.getPlayerState(gameId, userId);
    }

//    public Mono<ConcurrentMap<Long, PlayerState>> getAllPlayers(Long gameId) {
//        String playersKey = RedisKeys.gamePlayersKey(gameId);
//        return setOps.members(playersKey)
//                .map(obj -> Long.valueOf((String) obj))
//                .flatMap(userId -> playerStateService.getPlayerState(gameId, userId) // Use PlayerStateService
//                        .map(ps -> Map.entry(userId, ps)))
//                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue))
//                .onErrorResume(e -> Mono.just(new ConcurrentHashMap<>()));
//    }

    // ----------------------------
    // Disqualified Players
    // ----------------------------
    public Mono<Set<Long>> disqualifyPlayer(Long gameId, Long userId) {
        String disqualifiedKey = RedisKeys.gameDisqualifiedKey(gameId);

        return redis.hasKey(disqualifiedKey)
                .flatMap(exists -> {
                    Mono<Long> addUser = setOps.add(disqualifiedKey, userId.toString())
                            .onErrorResume(e -> {
                                log.error("Failed to add disqualified player {} for game {}: {}", userId, gameId, e.getMessage(), e);
                                return Mono.just(0L);
                            });

                    if (!exists) {
                        // First time: add + set TTL
                        return addUser.then(redis.expire(disqualifiedKey, GAME_STATE_TTL))
                                .thenMany(setOps.members(disqualifiedKey))
                                .map(str -> Long.valueOf((String) str))
                                .collect(Collectors.toSet());
                    } else {
                        // Just add
                        return addUser
                                .thenMany(setOps.members(disqualifiedKey))
                                .map(str -> Long.valueOf((String) str))
                                .collect(Collectors.toSet());
                    }

                });
    }


    public Mono<Set<Long>> getDisqualifiedPlayers(Long gameId) {
        String disqualifiedKey = RedisKeys.gameDisqualifiedKey(gameId);
        return setOps.members(disqualifiedKey)
                .map(obj -> Long.valueOf((String) obj))
                .collect(Collectors.toSet())
                .onErrorResume(e -> Mono.just(Set.of()));
    }

    // ----------------------------
    // Set Winner
    // ----------------------------
    public Mono<Boolean> setGameWinner(Long gameId, Long winnerId) {
        String gameKey = RedisKeys.gameStateKey(gameId);
        return Mono.when(
                        hashOps.put(gameKey, "winnerId", winnerId.toString()),
                        hashOps.put(gameKey, "ended", "true")
                )
                .thenReturn(true)
                .onErrorResume(e -> Mono.just(false));
    }


//    public Mono<GameState> getGameState(Long roomId) {
//        String gameKey = RedisKeys.gameStateKey(roomId);
//
//        return hashOps.entries(gameKey)
//                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
//                .flatMap(gameMeta -> {
//                    if (gameMeta.isEmpty()) {
//                        return Mono.empty();
//                    }
//
//                    GameState state = new GameState();
//                    state.setGameId(Long.valueOf(gameMeta.get("gameId").toString()));
//                    state.setRoomId(Long.valueOf(gameMeta.get("roomId").toString()));
//                    state.setStatus(GameStatus.valueOf(gameMeta.get("status").toString()));
//                    state.setStarted(Boolean.parseBoolean(gameMeta.get("started").toString()));
//                    state.setEnded(Boolean.parseBoolean(gameMeta.get("ended").toString()));
//                    state.getStopNumberDrawing().set(Boolean.parseBoolean(gameMeta.get("stopNumberDrawing").toString()));
//
//
//                    Mono<LinkedHashSet<Integer>> drawnNumbers = getDrawnNumbers(state.getGameId());
//                    Mono<Set<Long>> players = getAllPlayers(state.getGameId());
//                    Mono<Set<Long>> disqualified = getDisqualifiedPlayers(state.getGameId());
//                    Mono<List<CardInfo>> currentCardPool = cardPoolService.getCurrentPool(roomId).defaultIfEmpty(List.of()); // Placeholder if no capacity provided
//                    Mono<List<CardInfo>> nextCardPool = cardPoolService.getNextPool(roomId); // Placeholder if no capacity provided
//
//
//                    return Mono.zip(drawnNumbers, players, disqualified, currentCardPool, nextCardPool)
//                            .map(tuple -> {
//                                state.setDrawnNumber(tuple.getT1());
//                                state.setJoinedPlayers(tuple.getT2());
//                                state.setDisqualifiedPlayers(tuple.getT3());
//                                state.setCurrentCardPool(tuple.getT4());
//                                state.setNextCardPool(tuple.getT5());
//
//                                return state;
//                            });
//                })
//                .onErrorResume(e -> {
//                    log.error("Failed to get game state for room {}: {}", roomId, e.getMessage(), e);
//                    return Mono.empty();
//                });
//    }

    public Mono<GameState> getGameState(Long roomId) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        return hashOps.entries(gameKey)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(gameMeta -> {
                    if (gameMeta.isEmpty()) {
                        log.info("================================>>>> GAME IS EMPTY: {}", roomId);
                        return Mono.empty();
                    }

                    GameState state = new GameState();
                    state.setGameId(Long.valueOf(gameMeta.get("gameId").toString()));
                    state.setRoomId(Long.valueOf(gameMeta.get("roomId").toString()));
                    state.setStatus(GameStatus.valueOf(gameMeta.get("status").toString()));
                    state.setStarted(Boolean.parseBoolean(gameMeta.get("started").toString()));
                    state.setEnded(Boolean.parseBoolean(gameMeta.get("ended").toString()));
                    state.getStopNumberDrawing().set(Boolean.parseBoolean(gameMeta.get("stopNumberDrawing").toString()));

                    // Fetch all reactive parts
                    Mono<LinkedHashSet<Integer>> drawnNumbers = getDrawnNumbers(state.getGameId());
                    Mono<Set<Long>> players = getAllPlayers(state.getGameId());
                    Mono<Set<Long>> disqualified = getDisqualifiedPlayers(state.getGameId());
                    Mono<List<CardInfo>> currentCardPool = cardPoolService.getCurrentPool(roomId)
                            .defaultIfEmpty(List.of());
//                    Mono<List<CardInfo>> nextCardPool = cardPoolService.getNextPool(roomId)
//                            .defaultIfEmpty(List.of());

                    // Zip everything and apply reactive setters
                    return Mono.zip(drawnNumbers, players, disqualified, currentCardPool)
                            .flatMap(tuple ->
                                            state.setCurrentCardPool(tuple.getT4())
//                                            .then(state.setNextCardPool(tuple.getT5()))
                                                    .then(Mono.fromCallable(() -> {
                                                        state.setDrawnNumber(tuple.getT1());
                                                        state.setJoinedPlayers(tuple.getT2());
                                                        state.setDisqualifiedPlayers(tuple.getT3());
                                                        return state;
                                                    }))
                            );
                })
                .onErrorResume(e -> {
                    log.error("Failed to get game state for room {}: {}", roomId, e.getMessage(), e);
                    return Mono.empty();
                });
    }


    // ----------------------------
    // Delete GameState
    // ----------------------------
    public Mono<Boolean> deleteGameState(Long roomId) {
        String gameKey = RedisKeys.gameStateKey(roomId);

        return getGameState(roomId)
                .flatMap(state -> {
                    Long gameId = state.getGameId();
                    return redis.delete(
                            RedisKeys.gameStateKey(roomId),
                            RedisKeys.gameDrawnNumbersKey(gameId),
                            RedisKeys.gamePlayersKey(gameId),
                            RedisKeys.gameDisqualifiedKey(gameId),
                            RedisKeys.currentCardPoolKey(gameId)
                    ).map(count -> count > 0);
                })
                .onErrorResume(e -> Mono.just(false));
    }


    // ----------------------------
    // Player Management
    // ----------------------------
//    public Mono<Set<Long>> addPlayerToGame(Long gameId, Long userId) {
//        String playersKey = RedisKeys.gamePlayersKey(gameId);
//
//        return redis.hasKey(playersKey)
//                .flatMap(exists -> {
//                    Mono<Long> addUser = setOps.add(playersKey, userId.toString())
//                            .onErrorResume(e -> {
//                                log.error("Failed to add player {} for game {}: {}", userId, gameId, e.getMessage(), e);
//                                return Mono.just(0L);
//                            });
//                    if (!exists) {
//                        // First time: add + set TTL
//                        return addUser.then(redis.expire(playersKey, GAME_STATE_TTL))
//                                .thenMany(setOps.members(playersKey))
//                                .map(str -> Long.valueOf((String) str))
//                                .collect(Collectors.toSet());
//                    } else {
//                        // Just add
//                        return addUser
//                                .thenMany(setOps.members(playersKey))
//                                .map(str -> Long.valueOf((String) str))
//                                .collect(Collectors.toSet());
//                    }
//                });
//    }

    public Mono<Set<Long>> addPlayerToGame(Long gameId, Long userId) {
        String playersKey = RedisKeys.gamePlayersKey(gameId);

        return redis.hasKey(playersKey)
                .flatMap(exists -> {
                    Mono<Long> addUser = setOps.add(playersKey, userId.toString())
                            .onErrorResume(e -> {
                                log.error("Failed to add player {} for game {}: {}", userId, gameId, e.getMessage(), e);
                                return Mono.just(0L);
                            });

                    Mono<Set<Long>> resultMono = addUser
                            .then(Mono.defer(() -> setOps.members(playersKey) // fetch all members
                                    .map(obj -> Long.valueOf(obj.toString()))
                                    .collect(Collectors.toSet())
                            ));

                    if (!exists) {
                        return resultMono
                                .flatMap(set -> redis.expire(playersKey, GAME_STATE_TTL).thenReturn(set));
                    } else {
                        return resultMono;
                    }
                });
    }


    public Mono<Set<Long>> getAllPlayers(Long gameId) {
        String playersKey = RedisKeys.gamePlayersKey(gameId);
        return setOps.members(playersKey)
                .map(obj -> Long.valueOf(obj.toString()))  // safer conversion
                .collect(Collectors.toSet())
                .onErrorResume(e -> {
                    log.warn("Failed to fetch players for game {}: {}", gameId, e.getMessage());
                    return Mono.just(Set.of());
                });
    }


    public Mono<Boolean> removePlayerFromGame(Long gameId, Long userId) {
        String playersKey = RedisKeys.gamePlayersKey(gameId);
        return setOps.remove(playersKey, userId.toString())
                .thenReturn(true);
    }
}
