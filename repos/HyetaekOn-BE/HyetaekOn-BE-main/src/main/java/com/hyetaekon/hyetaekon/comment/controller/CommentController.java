package com.hyetaekon.hyetaekon.comment.controller;

import com.hyetaekon.hyetaekon.comment.dto.CommentCreateRequestDto;
import com.hyetaekon.hyetaekon.comment.dto.CommentListResponseDto;
import com.hyetaekon.hyetaekon.comment.service.CommentService;
import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 게시글 댓글 목록 조회 (페이징 지원)
    @GetMapping
    public ResponseEntity<Page<CommentListResponseDto>> getComments(
        @PathVariable("postId") Long postId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        Page<CommentListResponseDto> comments = commentService.getComments(postId, page, size);
        return ResponseEntity.ok(comments);
    }

    // 게시글에 댓글 작성
    @PostMapping
    public ResponseEntity<CommentListResponseDto> createComment(
        @PathVariable("postId") Long postId,
        @RequestBody CommentCreateRequestDto commentDto,
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        CommentListResponseDto createdComment = commentService.createComment(postId, userDetails.getId(), commentDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    // 대댓글 목록 조회
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Page<CommentListResponseDto>> getReplies(
        @PathVariable("postId") Long postId,
        @PathVariable("commentId") Long commentId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size) {
        Page<CommentListResponseDto> replies = commentService.getReplies(postId, commentId, page, size);
        return ResponseEntity.ok(replies);
    }

    //대댓글 작성
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<CommentListResponseDto> createReply(
        @PathVariable("postId") Long postId,
        @PathVariable("commentId") Long commentId,
        @RequestBody CommentCreateRequestDto commentDto,
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 대댓글의 부모 댓글 ID 설정
        commentDto.setParentId(commentId);

        CommentListResponseDto createdReply = commentService.createComment(postId, userDetails.getId(), commentDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReply);
    }

    // 댓글 삭제 (관리자나 댓글 작성자만 가능)
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable("commentId") Long commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {

        commentService.deleteComment(commentId, userDetails.getId(), userDetails.getRole());
        return ResponseEntity.noContent().build();
    }
}
