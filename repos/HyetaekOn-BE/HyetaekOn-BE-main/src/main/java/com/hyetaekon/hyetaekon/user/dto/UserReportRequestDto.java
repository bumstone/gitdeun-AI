package com.hyetaekon.hyetaekon.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReportRequestDto {
    @NotNull(message = "신고 대상 사용자 ID는 필수입니다.")
    private Long reportedUserId;

    @NotNull(message = "신고 사유는 필수입니다.")
    private String reason;

    private String content;
}
