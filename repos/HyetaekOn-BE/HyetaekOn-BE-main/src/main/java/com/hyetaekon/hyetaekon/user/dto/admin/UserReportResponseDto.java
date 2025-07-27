package com.hyetaekon.hyetaekon.user.dto.admin;

import lombok.Getter;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserReportResponseDto {
    private Long id;
    private String reporterNickname;
    private String reportedNickname;
    private String reason;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
