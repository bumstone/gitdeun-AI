package com.hyetaekon.hyetaekon.user.dto.admin;

import com.hyetaekon.hyetaekon.user.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserAdminResponseDto {
    private Long id;
    private String realId;
    private String nickname;
    private String name;

    private String gender;
    private LocalDate birthAt;
    private String city;
    private String state;

    private String levelName;
    private int point;

    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private String deleteReason;
    private LocalDateTime suspendStartAt;
    private LocalDateTime suspendEndAt;
    private String suspendReason;
}
