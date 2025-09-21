//package com.ebingo.backend.game.state;
//
//import com.ebingo.backend.game.enums.GamePattern;
//import com.ebingo.backend.game.enums.RoomStatus;
//import lombok.Data;
//
//import java.math.BigDecimal;
//
/// **
// * Represents a room where players wait before a game starts
// */
//@Data
//public class RoomState {
//    private Long roomId;
//    private String name;
//
//    // Players currently in the room (waiting to play)
/// /    private Set<Long> connectedPlayers = ConcurrentHashMap.newKeySet();
//
//    private int minPlayers;
//    private int capacity;
//
//    private RoomStatus status = RoomStatus.GAME_READY;
//
//    private BigDecimal entryFee;
//
//    private GamePattern pattern;
//
//    // Reference to active game (if any)
//    private Long activeGameId;
//    private GameState activeGameState;
//}
