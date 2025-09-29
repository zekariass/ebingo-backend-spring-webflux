package com.ebingo.backend.payment.dto;

import com.ebingo.backend.payment.enums.TransactionStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TransactionDto {
    private Long id;
    private Long userProfileId;
    private Long transferTo;
    private Long paymentMethodId;
    private TransactionType txnType;
    private BigDecimal txnAmount;
    private TransactionStatus status;
    private String description;
    private Instant createdAt;
}
