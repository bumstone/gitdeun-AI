package com.hyetaekon.hyetaekon.publicservice.controller;

import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.service.RecentVisitService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/services/recent")
@RequiredArgsConstructor
public class RecentVisitController {

    private final RecentVisitService recentVisitService;

    /**
     * 최근 방문한 서비스 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<PublicServiceListResponseDto>> getRecentVisits(
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "3") @Positive @Max(10) int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getId() : 0L;

        // 비로그인 사용자는 빈 목록 반환
        if (userId == 0L) {
            return ResponseEntity.ok(Page.empty(PageRequest.of(page, size)));
        }

        Page<PublicServiceListResponseDto> recentServices = recentVisitService.getRecentVisits(userId, page, size);
        return ResponseEntity.ok(recentServices);
    }

    /**
     * 방문 기록 전체 삭제
     */
    @DeleteMapping
    public ResponseEntity<Void> clearRecentVisits(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails != null) {
            recentVisitService.clearUserVisits(userDetails.getId());
        }
        return ResponseEntity.noContent().build();
    }
}
