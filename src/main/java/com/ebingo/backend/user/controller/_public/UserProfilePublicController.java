package com.ebingo.backend.user.controller._public;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.user.dto.UserProfileCreateDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "User Profile Public Controller", description = "User Profile Public Controller")
@RequestMapping("/api/v1/public/user-profile")
public class UserProfilePublicController {

    private final UserProfileService userProfileService;

    public UserProfilePublicController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping("/create")
    @Operation(summary = "Create user profile", description = "Create user profile")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> createUserProfile(
            @Parameter(required = true, description = "User profile")
            @Valid @RequestBody UserProfileCreateDto userProfileDto,
            ServerWebExchange exchange
    ) {
        return userProfileService.createUserProfile(userProfileDto)
                .map(userProfile -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .success(true)
                        .message("User profile created successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfile)
                        .build()
                )
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
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
