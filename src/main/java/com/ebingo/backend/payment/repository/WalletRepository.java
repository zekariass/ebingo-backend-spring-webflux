package com.ebingo.backend.payment.repository;

import com.ebingo.backend.payment.entity.Wallet;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface WalletRepository extends ReactiveCrudRepository<Wallet, Long> {

    Mono<Wallet> findByUserProfileId(Long userProfileId);
}

