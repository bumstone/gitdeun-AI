package com.hyetaekon.hyetaekon.user.controller;

import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import com.hyetaekon.hyetaekon.user.dto.UserReportRequestDto;
import com.hyetaekon.hyetaekon.user.service.UserReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users/reports")
@RequiredArgsConstructor
public class UserReportController {
    private final UserReportService userReportService;

    @PostMapping
    public ResponseEntity<Void> reportUser(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody UserReportRequestDto reportRequestDto
    ) {
        Long reporterId = userDetails.getId();
        userReportService.reportUser(reporterId, reportRequestDto);
        return ResponseEntity.ok().build();
    }
}
