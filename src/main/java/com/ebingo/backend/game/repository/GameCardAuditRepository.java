package com.ebingo.backend.game.repository;

import com.ebingo.backend.game.entity.GameCardAudit;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameCardAuditRepository extends ReactiveCrudRepository<GameCardAudit, Long> {}
