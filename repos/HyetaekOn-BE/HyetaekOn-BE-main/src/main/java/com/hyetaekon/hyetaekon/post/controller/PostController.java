package com.hyetaekon.hyetaekon.post.controller;

import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import com.hyetaekon.hyetaekon.post.dto.*;
import com.hyetaekon.hyetaekon.post.entity.PostType;
import com.hyetaekon.hyetaekon.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // PostType에 해당하는 게시글 목록 조회
    @GetMapping("/type")
    public ResponseEntity<Page<PostListResponseDto>> getPosts(
            @RequestParam(required = false, defaultValue = "ALL") String postType,
            @RequestParam(required = false) String keyword,  // 🔥 제목 검색 추가
            @RequestParam(defaultValue = "createdAt") String sortBy,  // 🔥 정렬 키워드 추가
            @RequestParam(defaultValue = "DESC") String direction,    // 🔥 정렬 방향 추가
            @PageableDefault(page = 0, size = 10) Pageable pageable) {

        PostType type = PostType.fromKoreanName(postType);

        if (type == PostType.ALL) {
            return ResponseEntity.ok(postService.getAllPosts(keyword, sortBy, direction, pageable));
        } else {
            return ResponseEntity.ok(postService.getPostsByType(type, keyword, sortBy, direction, pageable));
        }
    }

    // User, Admin에 따라 다른 접근 가능
    // ✅ 특정 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDto> getPost(
        @PathVariable Long postId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.getPostById(postId, userDetails.getId()));
    }

    // ✅ 게시글 생성
    @PostMapping
    public ResponseEntity<PostDetailResponseDto> createPost(
            @ModelAttribute PostCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        PostDetailResponseDto dto = postService.createPost(requestDto, userDetails.getId());
        return ResponseEntity.ok(dto);
    }


    // ✅ 게시글 수정 - 본인
    @PutMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDto> updatePost(
        @PathVariable Long postId,
        @ModelAttribute PostUpdateRequestDto updateDto,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.updatePost(postId, updateDto, userDetails.getId()));
    }

    // ✅ 게시글 삭제 (soft delete 방식 사용 가능) - 본인 혹은 관리자
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
        @PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails.getId(), userDetails.getRole());
        return ResponseEntity.noContent().build();
    }
}
