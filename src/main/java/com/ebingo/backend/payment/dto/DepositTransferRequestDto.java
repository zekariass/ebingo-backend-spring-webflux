package com.ebingo.backend.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DepositTransferRequestDto {
    @NotNull(message = "Email id is required")
    private String email;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
