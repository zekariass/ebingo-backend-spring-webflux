package com.ebingo.backend.game.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.game.dto.BingoClaimDto;
import com.ebingo.backend.game.service.BingoClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/secured/bingo-claims")
@Tag(name = "Bingo Claims API", description = "Bingo Claims API")
@RequiredArgsConstructor
public class BingoClaimController {

    private final BingoClaimService bingoClaimService;

    @GetMapping
    @Operation(summary = "Get paginated bingo claims", description = "Get paginated bingo claims")
    public Mono<ResponseEntity<ApiResponse<List<BingoClaimDto>>>> getPaginatedBingoClaims(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam String sortBy,
            ServerWebExchange exchange
    ) {
        String userSupabaseId = jwt.getSubject();
        return bingoClaimService.getPaginatedBingoClaim(userSupabaseId, page, size, sortBy)
                .collectList()
                .map(claims -> ApiResponse.<List<BingoClaimDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Transactions retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(claims)
                        .build())
                .map(ResponseEntity::ok);
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get bingo claim by id", description = "Get bingo claim by id")
    public Mono<ResponseEntity<ApiResponse<BingoClaimDto>>> getBingoClaimById(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam Long id,
            ServerWebExchange exchange
    ) {
        String userSupabaseId = jwt.getSubject();
        return bingoClaimService.getBingoClaimById(userSupabaseId, id)
                .map(claim -> ApiResponse.<BingoClaimDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("Bingo claim retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(claim)
                        .build())
                .map(ResponseEntity::ok);
    }
}
