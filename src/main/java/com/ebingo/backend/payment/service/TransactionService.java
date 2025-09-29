package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.TransactionDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Flux<TransactionDto> getPaginatedTransaction(String userSupabaseId, Integer page, Integer size, String sortBy);

    Mono<TransactionDto> getTransactionById(Long id, String userSupabaseId);
}
