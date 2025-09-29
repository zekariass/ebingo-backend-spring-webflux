package com.ebingo.backend.payment.mappers;

import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.payment.entity.Wallet;

public final class WalletMapper {
    public static WalletDto toDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .userProfileId(wallet.getUserProfileId())
                .totalDeposit(wallet.getTotalDeposit())
                .depositBalance(wallet.getDepositBalance())
                .pendingBalance(wallet.getPendingBalance())
                .welcomeBonus(wallet.getWelcomeBonus())
                .availableWelcomeBonus(wallet.getAvailableWelcomeBonus())
                .referralBonus(wallet.getReferralBonus())
                .availableReferralBonus(wallet.getAvailableReferralBonus())
                .totalPrizeAmount(wallet.getTotalPrizeAmount())
                .pendingWithdrawal(wallet.getPendingWithdrawal())
                .totalWithdrawal(wallet.getTotalWithdrawal())
                .totalAvailableBalance(wallet.getTotalAvailableBalance())
                .availableToWithdraw(wallet.getAvailableToWithdraw())
                .build();
    }
}
