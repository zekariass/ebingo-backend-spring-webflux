package com.ebingo.backend.payment.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.payment.dto.InitiateDepositRequest;
import com.ebingo.backend.payment.dto.TransactionDto;
import com.ebingo.backend.payment.dto.WithdrawRequestDto;
import com.ebingo.backend.payment.enums.TransactionStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import com.ebingo.backend.payment.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
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
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(required = true, description = "Page number") @RequestParam Integer page,
            @Parameter(required = false, description = "Page size") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(required = false, description = "Sort by") @RequestParam(defaultValue = "createdAt") String sortBy,
            ServerWebExchange exchange
    ) {
        String userSupabaseId = jwt.getSubject();
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
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> getTransactionById(
            @RequestParam String userSupabaseId,
            @Parameter(required = true, description = "Transaction ID") @RequestParam Long id,
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


    @PostMapping("/deposit/initiate-offline")
    @Operation(summary = "Initiate deposit", description = "Initiate deposit")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> initiateDeposit(
            @Parameter(required = true, description = "Amount") @RequestBody InitiateDepositRequest depositRequest,
            @AuthenticationPrincipal Jwt jwt,
            ServerWebExchange exchange
    ) {

        String userSupabaseId = jwt.getSubject();

        return transactionService.initiateOfflineDeposit(depositRequest, userSupabaseId)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Deposit initiated successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @PutMapping("/deposit/confirm-offline")
    @Operation(summary = "Confirm deposit offline", description = "Confirm deposit offline")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> confirmDepositOfflineByAdmin(
            @Parameter(required = true, description = "txnRef") @RequestParam String txnRef,
            @Parameter(required = false, description = "metaData") @RequestBody String metaData,
            @AuthenticationPrincipal Jwt jwt,
            ServerWebExchange exchange
    ) {

        String approverSupabaseId = jwt.getSubject();

        return transactionService.confirmDepositOfflineByAdmin(txnRef, metaData, approverSupabaseId)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Deposit confirmed successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @GetMapping("/by-status")
    @Operation(summary = "Get transactions by status", description = "Get transactions by status")
    public Mono<ResponseEntity<ApiResponse<List<TransactionDto>>>> getPaginatedTransactionsByStatusForAdmin(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam String sortBy,
            ServerWebExchange exchange
    ) {
        return transactionService.getPaginatedDepositsByStatusForAdmin(status, type, page, size, sortBy)
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


    @PutMapping("/deposit/{id}/cancel-offline")
    @Operation(summary = "Cancel deposit offline", description = "Cancel deposit offline")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> cancelDepositOffline(
            @Parameter(required = true, description = "id") @PathVariable Long id,
            ServerWebExchange exchange
    ) {

        return transactionService.cancelDepositOffline(id)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Deposit cancelled successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build()
                )
                .map(ResponseEntity::ok);
    }


    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw", description = "Withdraw")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WithdrawRequestDto withdrawRequestDto,
            ServerWebExchange exchange) {
        String userSupabaseId = jwt.getSubject();
        return transactionService.withdraw(withdrawRequestDto, userSupabaseId)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("Withdraw created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }


    @PostMapping("/withdraw/approve")
    @Operation(summary = "Approve Withdrawal", description = "Admin approves a pending withdrawal")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> approveWithdrawal(
            @RequestParam String txnRef,
            @AuthenticationPrincipal Jwt jwt,
            ServerWebExchange exchange) {

        String approverSupabaseId = jwt.getSubject();

        return transactionService.confirmWithdrawalByAdmin(txnRef, approverSupabaseId)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Withdrawal approved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }


    @PostMapping("/withdraw/reject")
    @Operation(summary = "Reject Withdrawal", description = "Admin rejects a pending withdrawal")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> rejectWithdrawal(
            @RequestParam String txnRef,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Jwt jwt,
            ServerWebExchange exchange) {

        String approverSupabaseId = jwt.getSubject();

        return transactionService.rejectWithdrawalByAdmin(txnRef, reason, approverSupabaseId)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Withdrawal rejected successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }


    @PatchMapping("/{txnRef}/change-status")
    @Operation(summary = "Change Transaction status", description = "Change Transaction status")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<TransactionDto>>> changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String txnRef,
            @RequestParam TransactionStatus status,
            ServerWebExchange exchange) {

        String approverSupabaseId = jwt.getSubject();
        return transactionService.changeTransactionStatus(txnRef, status, approverSupabaseId)
                .map(txn -> ApiResponse.<TransactionDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Withdrawal approved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(txn)
                        .build())
                .map(ResponseEntity::ok);
    }


}
