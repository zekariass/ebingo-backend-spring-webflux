package com.ebingo.backend.payment.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PaymentService {

    Mono<Boolean> processPayment(String userId, BigDecimal amount);

    Mono<Boolean> processRefund(String userId, BigDecimal amount);
}
