package com.ebingo.backend.game.mappers;

import com.ebingo.backend.game.entity.Game;
import com.ebingo.backend.game.state.GameState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class GameMapper {
    public static Game toEntity(GameState gs) {
        if (gs == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        Game game = new Game();
        game.setRoomId(gs.getRoomId());

        List<String> joinedPlayers = new ArrayList<>(gs.getJoinedPlayers());
        List<Integer> drawnNumbers = new ArrayList<>(gs.getDrawnNumbers());
        List<String> disqualifiedUsers = new ArrayList<>(gs.getDisqualifiedUsers());
        List<String> allCardIds = new ArrayList<>(gs.getAllCardIds());

        try {
            game.setJoinedPlayersJson(objectMapper.writeValueAsString(joinedPlayers));
            game.setDrawnNumberJson(objectMapper.writeValueAsString(drawnNumbers));
            game.setDisqualifiedUsersJson(objectMapper.writeValueAsString(disqualifiedUsers));
            game.setAllCardIdsJson(objectMapper.writeValueAsString(allCardIds));
        } catch (JsonProcessingException exc) {
            throw new RuntimeException("List to json mapping error.");
        }

        game.setStarted(gs.isStarted());
        game.setEnded(gs.isEnded());
        game.setStopNumberDrawing(gs.getStopNumberDrawing());
        game.setStatus(gs.getStatus());

        return game;
    }
}
