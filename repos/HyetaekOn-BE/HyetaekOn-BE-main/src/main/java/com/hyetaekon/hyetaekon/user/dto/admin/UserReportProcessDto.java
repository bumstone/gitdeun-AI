package com.hyetaekon.hyetaekon.user.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReportProcessDto {
    private boolean suspendUser; // 신고 처리 시 사용자 정지 여부

    // 사용자 정지시 필요한 정보 (suspendUser가 true일 때)
    private LocalDateTime suspendStartAt;
    private LocalDateTime suspendEndAt;
    private String suspendReason;
}
