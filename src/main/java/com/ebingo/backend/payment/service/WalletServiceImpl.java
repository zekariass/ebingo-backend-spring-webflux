package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.payment.entity.Wallet;
import com.ebingo.backend.payment.mappers.WalletMapper;
import com.ebingo.backend.payment.repository.WalletRepository;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final ReactiveTransactionManager transactionManager;
    private final UserProfileService userProfileService;

    @Override
    public Mono<WalletDto> createWallet(UserProfile userProfile) {
        log.info("Creating wallet for user with profile id: {}", userProfile.getId());

        Wallet wallet = new Wallet();
        wallet.setUserProfileId(userProfile.getId());

        // Use reactive transaction operator
        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        return walletRepository.save(wallet)
                .doOnNext(savedWallet -> log.info("Wallet created with id: {}", savedWallet.getId()))
                .map(WalletMapper::toDto)
                .as(operator::transactional);
    }

    @Override
    public Mono<WalletDto> getWalletByUserProfileId(Long userProfileId) {
        log.info("Getting wallet by user profile id: {}", userProfileId);
        return walletRepository.findByUserProfileId(userProfileId)
                .map(WalletMapper::toDto);
    }

    @Override
    public Mono<WalletDto> getWalletBySupabaseId(String userSupabaseId) {
        log.info("Getting wallet by user supabase id: {}", userSupabaseId);

        return userProfileService.getUserProfileBySupabaseId(UUID.fromString(userSupabaseId))
                .flatMap(up -> walletRepository.findByUserProfileId(up.getId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Wallet not found for user with supabase id: " + userSupabaseId)))
                .map(WalletMapper::toDto);
    }

}
