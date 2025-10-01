package com.ebingo.backend.payment.service;

import com.ebingo.backend.payment.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;


@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;

    public PaymentServiceImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Mono<Boolean> processPayment(String userId, BigDecimal amount) {
        // Implement payment processing logic here
        log.info("======================================>> Processing payment!");
        return Mono.just(true); // Placeholder implementation
    }

    @Override
    public Mono<Boolean> processRefund(String userId, Long gameId) {
        return Mono.just(true); // Placeholder implementation
    }
}
