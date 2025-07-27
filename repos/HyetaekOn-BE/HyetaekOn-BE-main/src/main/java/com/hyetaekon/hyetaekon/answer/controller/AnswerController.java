package com.hyetaekon.hyetaekon.answer.controller;

import com.hyetaekon.hyetaekon.answer.dto.AnswerDto;
import com.hyetaekon.hyetaekon.answer.service.AnswerService;
import com.hyetaekon.hyetaekon.common.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    // 답변 목록 조회
    // 게시글의 답변 목록 조회
    @GetMapping
    public ResponseEntity<Page<AnswerDto>> getAnswersByPostId(
        @PathVariable("postId") Long postId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AnswerDto> answers = answerService.getAnswersByPostId(postId, pageable);
        return ResponseEntity.ok(answers);
    }

    // 답변 작성
    @PostMapping
    public ResponseEntity<AnswerDto> createAnswer(
        @PathVariable("postId") Long postId,
        @RequestBody AnswerDto answerDto,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        AnswerDto createdAnswer = answerService.createAnswer(postId, answerDto, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAnswer);
    }

    // 답변 채택
    @PutMapping("/{answerId}/select")
    public ResponseEntity<Void> selectAnswer(
        @PathVariable("postId") Long postId,
        @PathVariable("answerId") Long answerId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 로그인한 사용자의 ID를 서비스에 전달
        answerService.selectAnswer(postId, answerId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    // 답변 삭제 (관리자와 작성자만 가능)
    @DeleteMapping("/{answerId}")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable("postId") Long postId,
            @PathVariable("answerId") Long answerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        answerService.deleteAnswer(postId, answerId, userDetails.getId(), userDetails.getRole());
        return ResponseEntity.noContent().build();
    }
}

