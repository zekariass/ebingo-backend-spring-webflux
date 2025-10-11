package com.ebingo.backend.user.service;


import com.ebingo.backend.user.dto.UserProfileCreateDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserProfileService {

    Mono<UserProfileDto> getUserProfileBySupabaseId(UUID supabaseId);

    Mono<UserProfileDto> createUserProfile(UserProfileCreateDto userProfileDto);

    Mono<UserProfileDto> getUserProfileById(@NotNull(message = "Receiver id is required") Long receiverId);

    Mono<UserProfileDto> getUserEmail(@NotNull(message = "Email id is required") String email);
}
