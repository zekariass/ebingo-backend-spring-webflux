package com.ebingo.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileCreateDto {
    @NotNull(message = "supabaseId is required")
    private UUID supabaseId;

    @NotBlank(message = "firstName is required")
    private String firstName;

    @NotBlank(message = "lastName is required")
    private String lastName;
    private String nickName;

    @NotBlank(message = "email is required")
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
}
