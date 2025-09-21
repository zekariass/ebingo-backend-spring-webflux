package com.ebingo.backend.user.dto;

import com.ebingo.backend.payment.dto.WalletDto;
import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.enums.UserStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserProfileDto {
    private Long id;
    private UUID supabaseId;
    private String firstName;
    private String lastName;
    private String nickName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private UserStatus status;
    private UserRole role;
    private Integer dailyFreePlayCount;
    private WalletDto wallet;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
