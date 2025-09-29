package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.TransactionDto;
import com.ebingo.backend.payment.mappers.TransactionMapper;
import com.ebingo.backend.payment.repository.TransactionRepository;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserProfileService userProfileService;

    @Override
    public Flux<TransactionDto> getPaginatedTransaction(String userSupabaseId, Integer page, Integer size, String sortBy) {
        log.info("Fetching transactions - Page: {}, Size: {}, SortBy: {}", page, size, sortBy);

        Mono<UserProfileDto> userProfileMono =
                userProfileService.getUserProfileBySupabaseId(UUID.fromString(userSupabaseId));

        int pageNumber = (page != null && page > 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;
        long offset = (long) pageNumber * pageSize;

        return userProfileMono.flatMapMany(up -> {
            if ("txnAmount".equalsIgnoreCase(sortBy)) {
                return transactionRepository.findByUserProfileIdOrderByTxnAmountDesc(up.getId(), pageSize, offset);
            } else if ("createdAt".equalsIgnoreCase(sortBy)) {
                return transactionRepository.findByUserProfileIdOrderByCreatedAtDesc(up.getId(), pageSize, offset);
            } else {
                return transactionRepository.findByUserProfileIdOrderByIdDesc(up.getId(), pageSize, offset);
            }
        }).map(TransactionMapper::toDto);
    }


    @Override
    public Mono<TransactionDto> getTransactionById(Long id, String userSupabaseId) {
        log.info("Fetching transaction by ID: {}", id);

        return userProfileService.getUserProfileBySupabaseId(UUID.fromString(userSupabaseId))
                .flatMap(up ->
                        transactionRepository.findByIdAndUserProfileId(id, up.getId())
                                .switchIfEmpty(Mono.error(
                                        new RuntimeException("Transaction not found with id: " + id + " for user: " + up.getId())
                                ))
                                .map(TransactionMapper::toDto)
                );
    }
}
