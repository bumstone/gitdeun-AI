package com.hyetaekon.hyetaekon.user.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSuspendRequestDto {
    @NotNull(message = "정지 시작 시간은 필수입니다.")
    private LocalDateTime suspendStartAt;

    @NotNull(message = "정지 종료 시간은 필수입니다.")
    private LocalDateTime suspendEndAt;

    @NotNull(message = "정지 사유는 필수입니다.")
    private String suspendReason;
}
