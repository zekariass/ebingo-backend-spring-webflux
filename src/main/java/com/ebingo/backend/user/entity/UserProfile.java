package com.ebingo.backend.user.entity;

import com.ebingo.backend.user.enums.UserRole;
import com.ebingo.backend.user.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_profile")
public class UserProfile {

    @Id
    private Long id;

    @Column("supabase_id")
    private UUID supabaseId;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("nick_name")
    private String nickName;

    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

    @Column("date_of_birth")
    private LocalDate dateOfBirth = LocalDate.of(2000, 1, 1);

    @Column("status")
    private UserStatus status;

    @Column("role")
    private UserRole role;

    @Column("daily_free_play_count")
    private Integer dailyFreePlayCount = 0;

    private Boolean isDeleted = false;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

}
