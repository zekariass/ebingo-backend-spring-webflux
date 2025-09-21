package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.user.entity.UserProfile;
import reactor.core.publisher.Mono;

public interface WalletService {
    Mono<WalletDto> createWallet(UserProfile userProfile);

    Mono<WalletDto> getWalletByUserProfileId(Long userProfileId);
}
