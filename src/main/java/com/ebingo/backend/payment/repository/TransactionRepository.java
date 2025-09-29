package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.Transaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionRepository extends ReactiveCrudRepository<Transaction, Long> {
    // CreatedAt sorting
    @Query("SELECT * FROM transaction " +
            "WHERE user_profile_id = :userProfileId " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByUserProfileIdOrderByCreatedAtDesc(Long userProfileId, int limit, long offset);

    // TxnAmount sorting
    @Query("SELECT * FROM transaction " +
            "WHERE user_profile_id = :userProfileId " +
            "ORDER BY txn_amount DESC " +
            "LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByUserProfileIdOrderByTxnAmountDesc(Long userProfileId, int limit, long offset);

    // Default (by id)
    @Query("SELECT * FROM transaction " +
            "WHERE user_profile_id = :userProfileId " +
            "ORDER BY id DESC " +
            "LIMIT :limit OFFSET :offset")
    Flux<Transaction> findByUserProfileIdOrderByIdDesc(Long userProfileId, int limit, long offset);

    @Query("SELECT * FROM transaction " +
            "WHERE id = :id " +
            "AND user_profile_id = :userProfileId")
    Mono<Transaction> findByIdAndUserProfileId(Long id, Long userProfileId);
}
