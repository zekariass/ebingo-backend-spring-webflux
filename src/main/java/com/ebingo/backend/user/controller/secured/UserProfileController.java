package com.ebingo.backend.user.controller.secured;

import com.ebingo.backend.common.dto.ApiResponse;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RestController
@Tag(name = "User Profile", description = "User Profile APIs")
@RequestMapping("/api/v1/secured/user-profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get user profile", description = "Get user profile")
    public Mono<ResponseEntity<ApiResponse<UserProfileDto>>> getUserProfile(
            Authentication authentication,
            ServerWebExchange exchange
    ) {
        UUID supabaseId = UUID.fromString(authentication.getName());

        return userProfileService.getUserProfileBySupabaseId(supabaseId)
                .map(userProfileDto -> ApiResponse.<UserProfileDto>builder()
                        .statusCode(HttpStatus.OK.value())
                        .success(true)
                        .message("User profile retrieved successfully")
                        .path(exchange.getRequest().getPath().value())
                        .timestamp(Instant.now())
                        .data(userProfileDto)
                        .build()
                )
                .map(response -> ResponseEntity.ok(response));
    }
}
