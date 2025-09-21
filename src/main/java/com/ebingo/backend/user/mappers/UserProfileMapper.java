package com.ebingo.backend.user.mappers;


import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.user.dto.UserProfileCreateDto;
import com.ebingo.backend.user.dto.UserProfileDto;
import com.ebingo.backend.user.dto.UserProfileUpdateDto;
import com.ebingo.backend.user.entity.UserProfile;
import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.enums.UserStatus;

public final class UserProfileMapper {

//    public static UserProfileDto toDto(UserProfile userProfile, WalletDto wallet) {
//        if (userProfile == null) return null;
//        return UserProfileDto.builder()
//                .id(userProfile.getId())
//                .supabaseId(userProfile.getSupabaseId())
//                .firstName(userProfile.getFirstName())
//                .lastName(userProfile.getLastName())
//                .nickName(userProfile.getNickName())
//                .email(userProfile.getEmail())
//                .phone(userProfile.getPhone())
//                .status(userProfile.getStatus())
//                .role(userProfile.getRole())
//                .freePlayCount(userProfile.getFreePlayCount())
//                .wallet(wallet)
//                .createdAt(userProfile.getCreatedAt())
//                .updatedAt(userProfile.getUpdatedAt())
//                .build();
//    }

    public static UserProfileDto toDto(UserProfile userProfile, WalletDto walletDto) {
        if (userProfile == null) return null;
        return UserProfileDto.builder()
                .id(userProfile.getId())
                .supabaseId(userProfile.getSupabaseId())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .nickName(userProfile.getNickName())
                .email(userProfile.getEmail())
                .phone(userProfile.getPhone())
                .dateOfBirth(userProfile.getDateOfBirth())
                .status(userProfile.getStatus())
                .role(userProfile.getRole())
                .dailyFreePlayCount(userProfile.getDailyFreePlayCount())
                .wallet(walletDto)
                .createdAt(userProfile.getCreatedAt())
                .updatedAt(userProfile.getUpdatedAt())
                .build();
    }

    public static UserProfile toEntity(UserProfileCreateDto userProfileDto) {
        if (userProfileDto == null) return null;
        UserProfile userProfile = new UserProfile();
        userProfile.setSupabaseId(userProfileDto.getSupabaseId());
        userProfile.setFirstName(userProfileDto.getFirstName());
        userProfile.setLastName(userProfileDto.getLastName());
        userProfile.setNickName(userProfileDto.getNickName());
        userProfile.setEmail(userProfileDto.getEmail());
        userProfile.setPhone(userProfileDto.getPhone());
        userProfile.setDateOfBirth(userProfileDto.getDateOfBirth());
        userProfile.setStatus(UserStatus.ACTIVE); // Default status
        userProfile.setRole(UserRole.PLAYER);
        return userProfile;
    }

    public static UserProfile toEntity(UserProfileUpdateDto userProfileDto, UserProfile existingUserProfile) {

        if (userProfileDto == null) return null;
        if (existingUserProfile == null) return null;
        if (userProfileDto.getFirstName() != null) {
            existingUserProfile.setFirstName(userProfileDto.getFirstName());
        }
        if (userProfileDto.getLastName() != null) {
            existingUserProfile.setLastName(userProfileDto.getLastName());
        }
        if (userProfileDto.getNickName() != null) {
            existingUserProfile.setNickName(userProfileDto.getNickName());
        }
        if (userProfileDto.getStatus() != null) {
            existingUserProfile.setStatus(userProfileDto.getStatus());
        }
        if (userProfileDto.getPhone() != null) {
            existingUserProfile.setPhone(userProfileDto.getPhone());
        }
        if (userProfileDto.getDateOfBirth() != null) {
            existingUserProfile.setDateOfBirth(userProfileDto.getDateOfBirth());
        }
        return existingUserProfile;
    }


}
