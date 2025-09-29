package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.payment.dto.TransactionDto;
import com.ebingo.backend.payment.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@Tag(name = "Transaction Secured Controller", description = "Transaction Secured Controller")
@RequestMapping("/api/v1/secured/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get all transactions with pagination", description = "Get all transactions with pagination")
    public Mono<ResponseEntity<ApiResponse<List<TransactionDto>>>> getPaginatedTransactions(
            @RequestParam String userSupabaseId,
            @RequestParam Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam String sortBy,
            ServerWebExchange exchange
    ) {
        return transactionService.getPaginatedTransaction(userSupabaseId, page, size, sortBy)
                .collectList()
                .map(txns -> ApiResponse.<List<TransactionDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Transactions retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txns)
                        .build())
                .map(ResponseEntity::ok);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Get transaction by ID")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> getPaymentMethodById(
            @RequestParam String userSupabaseId,
            @Parameter(required = true, description = "Room ID") @RequestParam Long id,
            ServerWebExchange exchange) {
        return transactionService.getTransactionById(id, userSupabaseId)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Transaction is retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build()
                )
                .map(ResponseEntity::ok);
    }
}
