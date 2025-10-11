package com.ebingo.backend.user.repository;

import com.ebingo.backend.user.entity.UserProfile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserProfileRepository extends ReactiveCrudRepository<UserProfile, Long> {

    // Fetch by supabaseId
    Mono<UserProfile> findBySupabaseId(UUID supabaseId);

    Mono<UserProfile> findByEmail(String email);

//    // Fetch wallet manually in service layer; no join fetch in R2DBC
//    default Mono<UserProfile> findBySupabaseIdWithWallet(UUID supabaseId) {
//        return findBySupabaseId(supabaseId)
//                .flatMap(userProfile -> {
//                    return Mono.just(userProfile); // placeholder
//                });
//    }
}
