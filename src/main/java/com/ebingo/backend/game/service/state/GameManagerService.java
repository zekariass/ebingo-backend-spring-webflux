//package com.ebingo.backend.game.service.state;
//
//import com.ebingo.backend.game.enums.RoomStatus;
//import com.ebingo.backend.game.state.GameState;
//import com.ebingo.backend.game.state.PlayerState;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//
//@Service
//@RequiredArgsConstructor
//public class GameManagerService {
//
//    private final RoomStateService roomStateService;
//    private final GameStateService gameStateService;
//    private final PlayerStateService playerStateService;
//
//    public Mono<Long> createGameFromRoom(Long roomId) {
//        return roomStateService.getRoomState(roomId)
//                .flatMap(roomState -> {
//                    GameState gameState = new GameState();
//                    gameState.setGameId(1L); // In real scenario, generate unique ID
//                    gameState.setRoomId(roomId);
//
//                    // Add all room players to game
//                    roomState.getConnectedPlayers().forEach(userId -> {
//                        PlayerState playerState = gameState.addPlayer(userId);
//                        // Initialize player state in Redis
//                        playerStateService.savePlayerState(gameState.getGameId(), playerState).subscribe();
//                    });
//
//                    return gameStateService.saveGameState(gameState)
//                            .then(roomStateService.getRoomState(roomId)
//                                    .flatMap(room -> {
//                                        room.setActiveGameId(gameState.getGameId());
//                                        room.setStatus(RoomStatus.GAME_READY);
//                                        return roomStateService.saveRoomState(room);
//                                    })
//                            )
//                            .thenReturn(gameState.getGameId());
//                });
//    }
//
//
//    public Mono<Boolean> markNumber(Long gameId, Long userId, String cardId, int number) {
//        return playerStateService.getPlayerState(gameId, userId)
//                .flatMap(playerState -> {
//                    playerState.markNumber(cardId, number);
//                    return playerStateService.savePlayerCard(gameId, userId, cardId,
//                            playerState.getCards().get(cardId));
//                });
//    }
//}
