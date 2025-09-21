package com.ebingo.backend.game.ws;

//import com.ebingo.backend.game.dto.WSMessage;
//import com.ebingo.backend.game.service.CardSelectionService;
//import com.ebingo.backend.game.service.GameService;
//import com.ebingo.backend.game.service.RedisPublisher;
//import com.ebingo.backend.game.service.CardPoolService;
//import com.ebingo.backend.game.state.PlayerState;
//import com.ebingo.backend.system.redis.RedisKeys;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.connection.ReactiveSubscription;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.data.redis.listener.ChannelTopic;
//import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
//import org.springframework.stereotype.Component;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.reactive.socket.WebSocketHandler;
//import org.springframework.web.reactive.socket.WebSocketMessage;
//import org.springframework.web.reactive.socket.WebSocketSession;
//import org.springframework.web.util.UriComponentsBuilder;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.core.publisher.Sinks;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class GameWebSocketHandler implements WebSocketHandler {
//
//    private final ReactiveRedisMessageListenerContainer listenerContainer;
//    private final ReactiveStringRedisTemplate redis;
//    private final ObjectMapper mapper;
//    private final CardPoolService cardPoolService;
//    private final CardSelectionService cardSelectionService;
//    private final RedisPublisher publisher;
//    private final GameService gameService;
//
//    private final Map<String, Sinks.Many<WSMessage>> sessionSinks = new java.util.concurrent.ConcurrentHashMap<>();
//
//    @Override
//    public Mono<Void> handle(WebSocketSession session) {
//        String sessionId = session.getId();
//        Sinks.Many<WSMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
//        sessionSinks.put(sessionId, sink);
//
//        MultiValueMap<String, String> params = UriComponentsBuilder
//                .fromUri(session.getHandshakeInfo().getUri())
//                .build()
//                .getQueryParams();
//
//        Long roomId = Optional.ofNullable(params.getFirst("roomId"))
//                .map(Long::valueOf)
//                .orElseThrow(() -> new RuntimeException("roomId query param required"));
//        Long gameId = Optional.ofNullable(params.getFirst("gameId")).map(Long::valueOf).orElse(null);
//
//        return Mono.deferContextual(ctx -> {
/// /           Long userId = ctx.get("userId");
//            Long userId = Optional.ofNullable(params.getFirst("userId"))
//                    .map(Long::valueOf)
//                    .orElseGet(() -> ctx.getOrDefault("userId", 1L)); // fallback 1L
//
//
//            Flux<String> roomFlux = listenerContainer.receive(ChannelTopic.of(RedisKeys.roomChannel(roomId)))
//                    .map(ReactiveSubscription.Message::getMessage);
//
//            Flux<String> gameFlux = (gameId != null)
//                    ? listenerContainer.receive(ChannelTopic.of(RedisKeys.gameChannel(gameId)))
//                    .map(ReactiveSubscription.Message::getMessage)
//                    : Flux.empty();
//
//            Flux<String> merged = Flux.merge(roomFlux, gameFlux);
//
//            merged.map(str -> {
//                        try {
//                            return mapper.readValue(str, WSMessage.class);
//                        } catch (Exception e) {
//                            log.error("Failed to parse Redis message: {}", str, e);
//                            return new WSMessage("error", Map.of("message", "invalid_message"));
//                        }
//                    })
//                    .subscribe(sink::tryEmitNext);
//
//            Flux<WebSocketMessage> outgoing = sink.asFlux()
//                    .map(msg -> {
//                        try {
//                            return session.textMessage(mapper.writeValueAsString(msg));
//                        } catch (Exception e) {
//                            log.error("Error serializing WS message", e);
//                            return session.textMessage("{}");
//                        }
//                    });
//
//            redis.opsForSet().add(RedisKeys.roomPlayersKey(roomId), userId.toString()).subscribe();
//
//            Mono<Void> incoming = session.receive()
//                    .map(WebSocketMessage::getPayloadAsText)
//                    .flatMap(text -> {
//                        try {
//                            WSMessage msg = mapper.readValue(text, WSMessage.class);
//                            return handleClientMessage(msg, roomId, gameId, userId, sessionId);
//                        } catch (Exception e) {
//                            log.error("Invalid WS message: {}", text, e);
//                            return publisher.publishEvent(RedisKeys.roomChannel(roomId),
//                                    Map.of("type", "error", "payload", Map.of("message", "invalid_message"))).then();
//                        }
//                    })
//                    .then();
//
//            return session.send(outgoing)
//                    .and(incoming)
//                    .doFinally(sig -> {
//                        redis.opsForSet().remove(RedisKeys.roomPlayersKey(roomId), userId.toString()).subscribe();
//                        sessionSinks.remove(sessionId);
//                    });
//        });
//    }
//
//    private Mono<Void> handleClientMessage(WSMessage msg, Long roomId, Long gameId, Long userId, String sessionId) {
//        String type = msg.getType();
//        Map<String, Object> payload = msg.getPayload() != null ? msg.getPayload() : Map.of();
//
//        switch (type) {
//            case "selectCard": {
//                String cardId = String.valueOf(payload.get("cardId"));
//                int maxCards = 2;
//                return cardSelectionService.claimCard(roomId, userId, cardId, maxCards)
//                        .then(publisher.publishEvent(
//                                RedisKeys.roomChannel(roomId),
//                                Map.of(
//                                        "type", "cardSelected",
//                                        "payload", Map.of(
//                                                "cardId", cardId,
//                                                "userId", userId
//                                        )
//                                )
//                        ))
//                        .then();
//            }
//
//            case "join": {
//                String joinCardId = (String) payload.get("cardId");
//                Map<String, Object> cardData = (Map<String, Object>) payload.get("cardData");
//                int roomCapacity = (Integer) payload.getOrDefault("roomCapacity", 100);
//                return gameService.playerJoin(gameId, roomId, userId, joinCardId, cardData, roomCapacity);
//            }
//
//            case "startGame": {
//                int countdown = (Integer) payload.getOrDefault("countdown", 30);
//                if (gameId == null) return Mono.error(new RuntimeException("gameId required to start game"));
//                return gameService.startCountdownByGameId(gameId, countdown);
//            }
//
//            case "claimBingo": {
//                if (gameId == null) return Mono.error(new RuntimeException("gameId required to claim bingo"));
//
//                String bingoCardId = (String) payload.get("cardId");
//                List<Integer> markedNumbers = (List<Integer>) payload.get("markedNumbers");
//                String pattern = (String) payload.get("pattern");
//
//                return gameService.claimBingo(gameId, userId, bingoCardId, markedNumbers, pattern)
//                        .flatMap(isWinner -> {
//                            PlayerState player = gameService.getPlayerState(gameId, userId);
//
//                            if (isWinner) {
//                                return publisher.publishEvent(
//                                        RedisKeys.gameChannel(gameId),
//                                        Map.of(
//                                                "type", "bingoWinner",
//                                                "payload", Map.of(
//                                                        "userId", userId,
//                                                        "cardId", bingoCardId,
//                                                        "markedNumbers", player.getMarkedNumbers(bingoCardId)
//                                                )
//                                        )
//                                ).then();
//                            } else {
//                                return publisher.publishEvent(
//                                        RedisKeys.gameChannel(gameId),
//                                        Map.of(
//                                                "type", "bingoInvalid",
//                                                "payload", Map.of(
//                                                        "userId", userId,
//                                                        "cardId", bingoCardId
//                                                )
//                                        )
//                                ).then();
//                            }
//                        });
//            }
//
//            default:
//                return publisher.publishEvent(
//                        RedisKeys.roomChannel(roomId),
//                        Map.of(
//                                "type", "error",
//                                "payload", Map.of("message", "unknown_action")
//                        )
//                ).then();
//        }
//    }
//
//
//    public void sendToSession(String sessionId, WSMessage msg) {
//        Optional.ofNullable(sessionSinks.get(sessionId)).ifPresent(s -> s.tryEmitNext(msg));
//    }
//}


import com.ebingo.backend.game.dto.WSMessage;
import com.ebingo.backend.game.service.CardPoolService;
import com.ebingo.backend.game.service.CardSelectionService;
import com.ebingo.backend.game.service.GameService;
import com.ebingo.backend.game.service.RedisPublisher;
import com.ebingo.backend.system.redis.RedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketHandler implements WebSocketHandler {

    private final ReactiveRedisMessageListenerContainer listenerContainer;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final CardPoolService cardPoolService;
    private final CardSelectionService cardSelectionService;
    private final RedisPublisher publisher;
    private final GameService gameService;

    private final Map<String, Sinks.Many<WSMessage>> sessionSinks = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        Sinks.Many<WSMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessionSinks.put(sessionId, sink);

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(session.getHandshakeInfo().getUri())
                .build()
                .getQueryParams();

        Long roomId = Optional.ofNullable(params.getFirst("roomId"))
                .map(Long::valueOf)
                .orElse(null);
        Long gameId = Optional.ofNullable(params.getFirst("gameId")).map(Long::valueOf).orElse(null);
        Long userId = Optional.ofNullable(params.getFirst("userId"))
                .map(Long::valueOf)
                .orElse(null);
        Integer capacity = Optional.ofNullable(params.getFirst("capacity"))
                .map(Integer::valueOf)
                .orElse(100);

        BigDecimal entryFee = Optional.ofNullable(params.getFirst("entryFee"))
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO);

        String username = Optional.ofNullable(params.getFirst("username"))
                .map(String::valueOf)
                .orElse("User: " + userId);


        if (roomId == null || userId == null) {
            return publishUserError(userId, "roomId and userId query params are required", "invalid_params");
        }

        return Mono.deferContextual(ctx -> {
            // Subscribe to room channel (broadcast)
            Flux<String> roomFlux = listenerContainer.receive(ChannelTopic.of(RedisKeys.roomChannel(roomId)))
                    .map(ReactiveSubscription.Message::getMessage);

            // Subscribe to game channel (broadcast)
            Flux<String> gameFlux = (gameId != null)
                    ? listenerContainer.receive(ChannelTopic.of(RedisKeys.gameChannel(gameId)))
                    .map(ReactiveSubscription.Message::getMessage)
                    : Flux.empty();

            // Subscribe to user-specific channel (private messages)
            Flux<String> userFlux = listenerContainer.receive(ChannelTopic.of(RedisKeys.userChannel(userId)))
                    .map(ReactiveSubscription.Message::getMessage);

            // Merge all channels
            Flux<String> merged = Flux.merge(roomFlux, gameFlux, userFlux);

            merged.map(str -> {
                        try {
                            return mapper.readValue(str, WSMessage.class);
                        } catch (Exception e) {
                            log.error("Failed to parse Redis message: {}", str, e);
                            return new WSMessage("error", Map.of("message", "invalid_message"));
                        }
                    })
                    .subscribe(sink::tryEmitNext);

            Flux<WebSocketMessage> outgoing = sink.asFlux()
                    .map(msg -> {
                        try {
                            return session.textMessage(mapper.writeValueAsString(msg));
                        } catch (Exception e) {
                            log.error("Error serializing WS message", e);
                            return session.textMessage("{}");
                        }
                    });

            redis.opsForSet().add(RedisKeys.roomPlayersKey(roomId), userId.toString()).subscribe();

            Mono<Void> incoming = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .flatMap(text -> {
                        try {
                            WSMessage msg = mapper.readValue(text, WSMessage.class);
                            return handleClientMessage(msg, roomId, gameId, userId, username, sessionId, capacity, entryFee);
                        } catch (Exception e) {
                            log.error("Invalid WS message: {}", text, e);
                            // Send error to specific user only
                            return publisher.publishUserEvent(userId,
                                    Map.of("type", "error", "payload", Map.of("message", "invalid_message"))).then();
                        }
                    })
                    .then();

            return session.send(outgoing)
                    .and(incoming)
                    .doFinally(sig -> {
                        redis.opsForSet().remove(RedisKeys.roomPlayersKey(roomId), userId.toString()).subscribe();
                        sessionSinks.remove(sessionId);
                        // Clean up user channel subscription is handled automatically by the listener container
                    });
        });
    }

    private Mono<Void> handleClientMessage(WSMessage msg, Long roomId, Long gameId, Long userId, String username, String sessionId, Integer capacity, BigDecimal entryFee) {
        String type = msg.getType();
        Map<String, Object> payload = msg.getPayload() != null ? msg.getPayload() : Map.of();

        switch (type) {
            case "getInitialGameState": {
                log.info("===========================>>> getInitialGameState");
                return gameService.getOrInitializeGame(roomId, userId, capacity)
                        .flatMap(gs -> publisher.publishUserEvent(userId, Map.of(
                                "type", "initialGameState",
                                "payload", gs
                        ))).then();
            }


            case "selectCard": {
                String cardId = String.valueOf(payload.get("cardId"));
                int maxCards = 2;
                return cardSelectionService.claimCard(roomId, gameId, userId, cardId, maxCards).then();
            }

            case "deselectCard": {
                String cardId = String.valueOf(payload.get("cardId"));
                return cardSelectionService.releaseCard(roomId, gameId, userId, cardId).then();
            }

            case "joinGame": {
                return gameService.playerJoin(roomId, gameId, userId, capacity, entryFee).then();
            }

            case "leaveGame": {
                return gameService.leaveGame(roomId, gameId, userId, entryFee);
            }

            case "markNumber": {
                return gameService.markNumber(roomId, gameId, userId, payload);
            }

            case "unmarkNumber": {
                return gameService.unmarkNumber(roomId, gameId, userId, payload);
            }

            case "bingoClaim": {
                if (gameId == null) return Mono.error(new RuntimeException("gameId required to claim bingo"));

                return gameService.claimBingo(roomId, gameId, userId, username, payload);
            }


            default:
                // Send unknown action error to specific user only
                return publisher.publishUserEvent(userId,
                        Map.of(
                                "type", "error",
                                "payload", Map.of("message", "unknown_action")
                        )
                ).then();
        }
    }

    public void sendToSession(String sessionId, WSMessage msg) {
        Optional.ofNullable(sessionSinks.get(sessionId)).ifPresent(s -> s.tryEmitNext(msg));
    }


    private Mono<Void> publishUserError(Long userId, String message, String errorType) {
        return publisher.publishUserEvent(userId,
                Map.of(
                        "type", "error",
                        "payload", Map.of(
                                "message", message,
                                "errorType", errorType
                        )
                )).then();
    }
}


