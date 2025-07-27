package com.hyetaekon.hyetaekon.recommend.controller;

import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import com.hyetaekon.hyetaekon.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/posts/{postId}/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    // 북마크 추가
    @PostMapping
    public ResponseEntity<Void> addBookmark(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        recommendService.addRecommend(postId, customUserDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 북마크 제거
    @DeleteMapping
    public ResponseEntity<Void> removeBookmark(
        @PathVariable("postId") Long postId,
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        recommendService.removeRecommend(postId, customUserDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
