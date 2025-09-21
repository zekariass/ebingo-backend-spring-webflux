package com.ebingo.backend.user.dto;

import com.ebingo.backend.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateDto {
    @NotNull(message = "id is required")
    private Long id;

    private String firstName;
    private String lastName;
    private String nickName;
    private UserStatus status;
    private String phone;
    private LocalDate dateOfBirth;
}
