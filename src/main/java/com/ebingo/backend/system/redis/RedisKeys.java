package com.ebingo.backend.system.redis;

public final class RedisKeys {
    public static String currentCardPoolKey(Long roomId) {
        return "current:game:pool:room" + roomId;
    }

//    public static String nextCardPoolKey(Long roomId) {
//        return "next:game:pool:room" + roomId;
//    }

    public static String roomCardKey(Long roomId, String cardId) {
        return "room:" + roomId + ":card:" + cardId;
    }

    public static String cardOwnerKey(Long gameId, String cardId) {
        return "game:" + gameId + ":card:" + cardId + ":owner";
    }

    public static String roomCardsSetKey(Long roomId) {
        return "room:" + roomId + ":cards";
    }

    public static String userOwnedCardsKey(Long gameId, Long userId) {
        return "game:" + gameId + ":owner:" + userId + ":cards";
    }

    public static String roomChannel(Long roomId) {
        return "room:" + roomId + ":channel";
    }

    public static String gameChannel(Long gameId) {
        return "game:" + gameId + ":channel";
    }

    public static String userChannel(Long userId) {
        return "user:" + userId + ":channel";
    }


    public static String userLockKey(Long gameId, Long userId) {
        return "game:" + gameId + ":user:" + userId + ":lock";
    }

    public static String cardLockKey(Long gameId, String cardId) {
        return "game:" + gameId + ":card:" + cardId + ":claim:lock";
    }


    // ROOM STATE

    public static String roomPlayersKey(Long roomId) {
        return "room:" + roomId + ":players";
    }

    // GAME STATE
    public static String gameStateKey(Long roomId) {
        return "room:" + roomId + ":game:state:";
    }

    public static String gameDrawnNumbersKey(Long gameId) {
        return "game:" + gameId + ":drawnNumbers";
    }

    public static String gamePlayersKey(Long gameId) {
        return "game:" + gameId + ":players";
    }

    public static String gameDisqualifiedKey(Long gameId) {
        return "game:" + gameId + ":disqualified";
    }

    // PLAYER STATE (per game)

    public static String playerCardsKey(Long gameId, Long userId) {
        return "game:" + gameId + ":player:" + userId + ":cards";
    }

    public static String playerMarkedNumbersKey(Long gameId, Long userId, String cardId) {
        cardId = cardId.replace("-", "_");
        return "game:" + gameId + ":player:" + userId + ":card:" + cardId + ":marked";
    }


    public static String gameInitLockKey(Long roomId) {
        return "room:" + roomId + ":gameInitLock";
    }

    public static String gameDrawingLockKey(Long gameId) {
        return "game:" + gameId + ":drawingLock";
    }

    public static String selectedCardsKey(long gameId) {
        return "game:" + gameId + ":selectedCardsIds";
    }
}

