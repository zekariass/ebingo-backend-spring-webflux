package com.ebingo.backend.user.service;


import com.ebingo.backend.user.dto.UserProfileCreateDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserProfileService {

    Mono<UserProfileDto> getUserProfileBySupabaseId(UUID supabaseId);

    Mono<UserProfileDto> createUserProfile(UserProfileCreateDto userProfileDto);
}
