package com.ebingo.backend.payment.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PaymentService {

    Mono<Boolean> processPayment(Long userId, BigDecimal amount);

    Mono<Boolean> processRefund(Long userId, BigDecimal amount);
}
