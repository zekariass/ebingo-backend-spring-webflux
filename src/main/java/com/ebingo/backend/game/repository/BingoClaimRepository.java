package com.ebingo.backend.game.repository;


import com.ebingo.backend.game.entity.BingoClaim;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface BingoClaimRepository extends ReactiveCrudRepository<BingoClaim, Long> { }
