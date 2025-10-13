package com.ebingo.backend.user.controller._public;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.payment.service.WalletService;
import com.ebingo.backend.user.dto.UserProfileCreateDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.mappers.UserProfileMapper;
import com.ebingo.backend.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "User Profile Public Controller", description = "User Profile Public Controller")
@RequestMapping("/api/v1/public/user-profile")
@Slf4j
public class UserProfilePublicController {

    private final UserProfileService userProfileService;
    private final WalletService walletService;
    private final ReactiveTransactionManager transactionManager;


    public UserProfilePublicController(UserProfileService userProfileService, WalletService walletService, ReactiveTransactionManager transactionManager) {
        this.userProfileService = userProfileService;
        this.walletService = walletService;
        this.transactionManager = transactionManager;
    }

//    @PostMapping("/create")
//    @Operation(summary = "Create user profile", description = "Create user profile")
//    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> createUserProfile(
//            @Parameter(required = true, description = "User profile")
//            @Valid @RequestBody UserProfileCreateDto userProfileDto,
//            ServerWebExchange exchange
//    ) {
//        return userProfileService.createUserProfile(userProfileDto)
//                .flatMap(userProfile -> walletService.createWallet(UserProfileMapper.toEntity(userProfile))
//                        .thenReturn(userProfile)
//                )
//                .map(userProfile -> ApiResponse.<UserProfileDto>builder()
//                        .statusCode(HttpStatus.CREATED.value())
//                        .success(true)
//                        .message("User profile created successfully")
//                        .path(exchange.getRequest().getPath().value())
//                        .timestamp(Instant.now())
//                        .data(userProfile)
//                        .build()
//                )
//                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
//    }


    @PostMapping("/create")
    @Operation(summary = "Create user profile", description = "Create user profile")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> createUserProfile(
            @Parameter(required = true, description = "User profile")
            @Valid @RequestBody UserProfileCreateDto userProfileDto,
            ServerWebExchange exchange
    ) {
        // Define transactional operator (you should already have it injected in the service/controller)
        TransactionalOperator operator = TransactionalOperator.create(transactionManager);

        Mono<UserProfileDto> createUserAndWallet = userProfileService.createUserProfile(userProfileDto)
                .flatMap(userProfile ->
                        walletService.createWallet(UserProfileMapper.toEntity(userProfile))
                                .thenReturn(userProfile)
                );

        // Wrap both in one transaction
        return operator.transactional(createUserAndWallet)
                .map(userProfile -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("User profile created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfile)
                        .build()
                )
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnSubscribe(s -> log.info("Creating user profile and wallet"))
                .doOnError(e -> log.error("Failed to create user + wallet: {}", e.getMessage(), e));
    }


    @GetMapping("/me")
    @Operation(summary = "Get user profile", description = "Get user profile")
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        return Mono.just(Map.of(
                "sub", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "claims", jwt.getClaims()
        ));
    }
}
