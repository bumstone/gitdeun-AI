package com.hyetaekon.hyetaekon.publicservice.controller.mongodb;

import com.hyetaekon.hyetaekon.post.dto.PostListResponseDto;
import com.hyetaekon.hyetaekon.post.service.PostService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.dto.mongodb.ServiceSearchCriteriaDto;
import com.hyetaekon.hyetaekon.publicservice.service.mongodb.ServiceSearchHandler;
import com.hyetaekon.hyetaekon.common.util.AuthenticateUser;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/mongo/search")
@RequiredArgsConstructor
public class SearchInfoController {
    private final ServiceSearchHandler searchService;
    private final PostService postService;
    private final AuthenticateUser authenticateUser;

    // 검색 API (로그인/비로그인 통합)
    @GetMapping("/services")
    public ResponseEntity<Page<PublicServiceListResponseDto>> searchServices(
        @RequestParam(name = "searchTerm", required = false, defaultValue = "") String searchTerm,
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "9") @Positive @Max(50) int size
    ) {
        // 검색 조건 생성
        ServiceSearchCriteriaDto searchCriteria = ServiceSearchCriteriaDto.builder()
            .searchTerm(searchTerm)
            .pageable(PageRequest.of(page, size))
            .build();

        // 사용자 인증 여부 확인
        Long userId = authenticateUser.authenticateUserId();

        // 인증된 사용자면 맞춤 검색, 아니면 기본 검색
        if (userId != 0L) {
            return ResponseEntity.ok(searchService.searchPersonalizedServices(searchCriteria, userId));
        } else {
            return ResponseEntity.ok(searchService.searchServices(searchCriteria));
        }
    }

    // 게시글 제목 검색(통합검색)
    @GetMapping("/posts")
    public ResponseEntity<Page<PostListResponseDto>> searchPosts(
        @RequestParam String searchTerm,
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "9") @Positive @Max(50) int size) {

        return ResponseEntity.ok(postService.getAllPosts(
            searchTerm, "createdAt", "DESC", PageRequest.of(page, size)));
    }

    // 자동완성 API(서비스에 대해서만)
    @GetMapping("/services/autocomplete")
    public ResponseEntity<List<String>> getAutocompleteResults(
        @RequestParam(name = "word") String word
    ) {
        return ResponseEntity.ok(searchService.getAutocompleteResults(word));
    }
}