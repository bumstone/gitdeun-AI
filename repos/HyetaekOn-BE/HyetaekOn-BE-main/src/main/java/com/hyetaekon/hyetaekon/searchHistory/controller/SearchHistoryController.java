package com.hyetaekon.hyetaekon.searchHistory.controller;

import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import com.hyetaekon.hyetaekon.searchHistory.Dto.SearchHistoryDto;
import com.hyetaekon.hyetaekon.searchHistory.Service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search/history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    /**
     * 현재 로그인한 사용자의 검색 기록 조회 (최신 6개)
     */
    @GetMapping
    public ResponseEntity<List<SearchHistoryDto>> getSearchHistories(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(searchHistoryService.getUserSearchHistories(userDetails.getId()));
    }

    /**
     * 특정 검색 기록 삭제
     */
    @DeleteMapping("/{historyId}")
    public ResponseEntity<Void> deleteSearchHistory(
        @PathVariable("historyId") String historyId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        searchHistoryService.deleteSearchHistory(userDetails.getId(), historyId);
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 검색 기록 삭제
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllSearchHistories(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        searchHistoryService.deleteAllSearchHistories(userDetails.getId());
        return ResponseEntity.ok().build();
    }
}
