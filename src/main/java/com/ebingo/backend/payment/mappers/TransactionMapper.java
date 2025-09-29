package com.ebingo.backend.payment.mappers;

import com.ebingo.backend.payment.dto.TransactionDto;
import com.ebingo.backend.payment.entity.Transaction;

public final class TransactionMapper {
    public static TransactionDto toDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .userProfileId(transaction.getUserProfileId())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .paymentMethodId(transaction.getPaymentMethodId())
                .transferTo(transaction.getTransferTo())
                .txnType(transaction.getTxnType())
                .txnAmount(transaction.getTxnAmount())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
