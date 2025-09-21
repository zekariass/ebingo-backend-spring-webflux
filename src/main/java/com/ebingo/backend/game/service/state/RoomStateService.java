//package com.ebingo.backend.game.service.state;
//
//import com.ebingo.backend.game.enums.GamePattern;
//import com.ebingo.backend.game.enums.RoomStatus;
//import com.ebingo.backend.game.state.RoomState;
//import com.ebingo.backend.system.redis.RedisKeys;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.ReactiveHashOperations;
//import org.springframework.data.redis.core.ReactiveRedisTemplate;
//import org.springframework.data.redis.core.ReactiveSetOperations;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//import java.math.BigDecimal;
//import java.time.Duration;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class RoomStateService {
//
//    private final ReactiveRedisTemplate<String, Object> redis;
//    private final ReactiveHashOperations<String, String, Object> hashOps;
//    private final ReactiveSetOperations<String, Object> setOps;
//
//    private static final Duration ROOM_TTL = Duration.ofHours(24);
//
//    // ------------------------
//    // Save / Update RoomState
//    // ------------------------
//    public Mono<Boolean> saveRoomState(RoomState roomState) {
//        String roomKey = RedisKeys.roomStateKey(roomState.getRoomId());
//        String playersKey = RedisKeys.roomPlayersKey(roomState.getRoomId());
//
//        Map<String, Object> roomData = Map.of(
//                "roomId", roomState.getRoomId(),
//                "name", roomState.getName(),
//                "minPlayers", roomState.getMinPlayers(),
//                "capacity", roomState.getCapacity(),
//                "status", roomState.getStatus().name(),
//                "entryFee", roomState.getEntryFee(),
//                "pattern", roomState.getPattern().name(),
//                "activeGameId", roomState.getActiveGameId() != null ? roomState.getActiveGameId() : "",
//                "connectedPlayersCount", roomState.getConnectedPlayers().size()
//        );
//
//        return Mono.when(
//                        // Save room metadata
//                        hashOps.putAll(roomKey, roomData)
//                                .then(redis.expire(roomKey, ROOM_TTL)),
//
//                        // Save connected players
//                        redis.delete(playersKey)
//                                .then(setOps.add(playersKey, roomState.getConnectedPlayers().stream()
//                                        .map(String::valueOf)
//                                        .toArray()))
//                                .then(redis.expire(playersKey, ROOM_TTL)),
//
//                        // Add room to activeRooms set
//                        redis.opsForSet().add(RedisKeys.activeRoomsKey(), roomState.getRoomId().toString())
//                )
//                .thenReturn(true)
//                .onErrorResume(e -> {
//                    log.error("Failed to save room state {}: {}", roomState.getRoomId(), e.getMessage(), e);
//                    return Mono.just(false);
//                });
//    }
//
//    // ------------------------
//    // Get RoomState
//    // ------------------------
//    public Mono<RoomState> getRoomState(Long roomId) {
//        String roomKey = RedisKeys.roomStateKey(roomId);
//
//        return hashOps.entries(roomKey)
//                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
//                .flatMap(data -> {
//                    if (data.isEmpty()) return Mono.empty();
//
//                    RoomState roomState = new RoomState();
//                    roomState.setRoomId(Long.valueOf(data.get("roomId").toString()));
//                    roomState.setName(data.get("name").toString());
//                    roomState.setMinPlayers(Integer.parseInt(data.get("minPlayers").toString()));
//                    roomState.setCapacity(Integer.parseInt(data.get("capacity").toString()));
//                    roomState.setStatus(RoomStatus.valueOf(data.get("status").toString()));
//                    roomState.setEntryFee(new BigDecimal(data.get("entryFee").toString()));
//                    roomState.setPattern(GamePattern.valueOf(data.get("pattern").toString()));
//
//                    if (data.get("activeGameId") != null && !data.get("activeGameId").toString().isEmpty()) {
//                        roomState.setActiveGameId(Long.valueOf(data.get("activeGameId").toString()));
//                    }
//
//                    return getRoomPlayers(roomId)
//                            .map(players -> {
//                                roomState.setConnectedPlayers(players);
//                                return roomState;
//                            });
//                })
//                .onErrorResume(e -> {
//                    log.error("Failed to get room state {}: {}", roomId, e.getMessage(), e);
//                    return Mono.empty();
//                });
//    }
//
//    // ------------------------
//    // Get connected players
//    // ------------------------
//    public Mono<Set<Long>> getRoomPlayers(Long roomId) {
//        String playersKey = RedisKeys.roomPlayersKey(roomId);
//        return setOps.members(playersKey)
//                .map(obj -> Long.valueOf(obj.toString()))
//                .collect(Collectors.toSet())
//                .onErrorResume(e -> {
//                    log.error("Failed to get room players {}: {}", roomId, e.getMessage(), e);
//                    return Mono.just(Set.of());
//                });
//    }
//
//    // ------------------------
//    // Add player (atomic via Redis operations)
//    // ------------------------
//    public Mono<Boolean> addPlayerToRoom(Long roomId, Long userId) {
//        String playersKey = RedisKeys.roomPlayersKey(roomId);
//        String roomKey = RedisKeys.roomStateKey(roomId);
//
//        return Mono.when(
//                        setOps.add(playersKey, userId.toString()), // atomic
//                        hashOps.increment(roomKey, "connectedPlayersCount", 1), // atomic
//                        redis.expire(playersKey, ROOM_TTL),
//                        redis.expire(roomKey, ROOM_TTL)
//                )
//                .thenReturn(true)
//                .onErrorResume(e -> {
//                    log.error("Failed to add player {} to room {}: {}", userId, roomId, e.getMessage(), e);
//                    return Mono.just(false);
//                });
//    }
//
//    // ------------------------
//    // Remove player (atomic via Redis operations)
//    // ------------------------
//    public Mono<Boolean> removePlayerFromRoom(Long roomId, Long userId) {
//        String playersKey = RedisKeys.roomPlayersKey(roomId);
//        String roomKey = RedisKeys.roomStateKey(roomId);
//
//        return Mono.when(
//                        setOps.remove(playersKey, userId.toString()), // atomic
//                        hashOps.increment(roomKey, "connectedPlayersCount", -1), // atomic
//                        redis.expire(playersKey, ROOM_TTL),
//                        redis.expire(roomKey, ROOM_TTL)
//                )
//                .thenReturn(true)
//                .onErrorResume(e -> {
//                    log.error("Failed to remove player {} from room {}: {}", userId, roomId, e.getMessage(), e);
//                    return Mono.just(false);
//                });
//    }
//
//    // ------------------------
//    // Check if room can start game
//    // ------------------------
//    public Mono<Boolean> canStartGame(Long roomId) {
//        String roomKey = RedisKeys.roomStateKey(roomId);
//
//        Mono<Object> playersCount = hashOps.get(roomKey, "connectedPlayersCount");
//        Mono<Object> minPlayers = hashOps.get(roomKey, "minPlayers");
//        Mono<Object> status = hashOps.get(roomKey, "status");
//
//        return Mono.zip(playersCount, minPlayers, status)
//                .map(tuple -> {
/// /                    long count = tuple.getT1() != null ? Long.parseLong(tuple.getT1().toString()) : 0;
/// /                    int min = tuple.getT2() != null ? Integer.parseInt(tuple.getT2().toString()) : 0;
/// /                    RoomStatus roomStatus = tuple.getT3() != null ? RoomStatus.valueOf(tuple.getT3().toString()) : RoomStatus.GAME_READY;
//                    long count = Long.parseLong(tuple.getT1().toString());
//                    int min = Integer.parseInt(tuple.getT2().toString());
//                    RoomStatus roomStatus = RoomStatus.valueOf(tuple.getT3().toString());
//                    return count >= min && roomStatus == RoomStatus.GAME_READY;
//                })
//                .onErrorResume(e -> {
//                    log.error("Failed to check canStartGame for room {}: {}", roomId, e.getMessage(), e);
//                    return Mono.just(false);
//                });
//    }
//}
//
