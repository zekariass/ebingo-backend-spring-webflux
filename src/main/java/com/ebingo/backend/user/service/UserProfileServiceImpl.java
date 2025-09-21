package com.ebingo.backend.user.service;

import com.ebingo.backend.payment.service.WalletService;
import com.ebingo.backend.system.exceptions.ResourceNotFoundException;
import com.ebingo.backend.user.dto.UserProfileCreateDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.mappers.UserProfileMapper;
import com.ebingo.backend.user.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final WalletService walletService;
    private final ReactiveTransactionManager transactionManager;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository,
                                  WalletService walletService, ReactiveTransactionManager transactionManager) {
        this.userProfileRepository = userProfileRepository;
        this.walletService = walletService;
        this.transactionManager = transactionManager;
    }

    @Override
    public Mono<UserProfileDto> getUserProfileBySupabaseId(UUID supabaseId) {
        log.info("Getting user profile by supabaseId: {}", supabaseId);

        return userProfileRepository.findBySupabaseId(supabaseId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .flatMap(userProfile ->
                        walletService.getWalletByUserProfileId(userProfile.getId())
                                .map(walletDto -> UserProfileMapper.toDto(userProfile, walletDto))
                );
    }

    @Override
    public Mono<UserProfileDto> createUserProfile(UserProfileCreateDto userProfileDto) {
        log.info("Creating user profile: {}", userProfileDto);

        UserProfile userProfile = UserProfileMapper.toEntity(userProfileDto);

        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        Mono<UserProfileDto> mono = userProfileRepository.save(userProfile)
                .flatMap(savedUser ->
                        walletService.createWallet(savedUser)
                                .map(walletDto -> UserProfileMapper.toDto(savedUser, walletDto))
                ).doOnError(e -> log.error("Error creating user profile: {}", userProfileDto, e));

        return mono.as(operator::transactional);
    }

}
