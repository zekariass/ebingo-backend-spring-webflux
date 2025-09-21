package com.ebingo.backend.game.repository;

import com.ebingo.backend.game.entity.GameEntry;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface GameEntryRepository extends ReactiveCrudRepository<GameEntry, Long> {

    @Query("SELECT * FROM game_entries WHERE game_id = :gameId AND user_profile_id = :userId")
    Flux<GameEntry> findByGameIdAndUserProfileId(Long gameId, Long userId);

    @Query("SELECT * FROM game_entries WHERE game_id = :gameId")
    Flux<GameEntry> findByGameId(Long gameId);
}
