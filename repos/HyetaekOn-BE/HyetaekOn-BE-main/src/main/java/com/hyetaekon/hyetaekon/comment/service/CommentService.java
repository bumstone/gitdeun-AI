package com.hyetaekon.hyetaekon.comment.service;

import com.hyetaekon.hyetaekon.comment.dto.CommentCreateRequestDto;
import com.hyetaekon.hyetaekon.comment.dto.CommentListResponseDto;
import com.hyetaekon.hyetaekon.comment.entity.Comment;
import com.hyetaekon.hyetaekon.comment.mapper.CommentMapper;
import com.hyetaekon.hyetaekon.comment.repository.CommentRepository;
import com.hyetaekon.hyetaekon.post.entity.Post;
import com.hyetaekon.hyetaekon.post.repository.PostRepository;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    /**
     * 게시글의 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CommentListResponseDto> getComments(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));

        return commentRepository.findByPostAndParentIdIsNull(post, pageable)
            .map(commentMapper::toResponseDto);
    }

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentListResponseDto createComment(Long postId, Long userId, CommentCreateRequestDto requestDto) {
        // 사용자와 게시글 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));

        // 엔티티 생성 및 설정
        Comment comment = commentMapper.toEntity(requestDto);
        comment.setUser(user);
        comment.setPost(post);

        // 저장 및 DTO 변환
        comment = commentRepository.save(comment);
        return commentMapper.toResponseDto(comment);
    }

    /**
     * 대댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CommentListResponseDto> getReplies(Long postId, Long commentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));

        return commentRepository.findByPostAndParentId(post, commentId, pageable)
            .map(commentMapper::toResponseDto);
    }

    /**
     * 댓글 삭제 (본인 또는 관리자만 가능)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId, String role) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다"));

        // 작성자 또는 관리자 확인
        boolean isOwner = comment.getUser().getId().equals(userId);
        boolean isAdmin = "ROLE_ADMIN".equals(role);

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("댓글 삭제 권한이 없습니다");
        }

        // Soft Delete 처리
        comment.delete();
        commentRepository.save(comment);
    }

    /*public CommentDto createReply(Long postId, Long commentId, CommentDto commentDto) {
        Comment reply = commentMapper.toEntity(commentDto);
        reply.setPostId(postId);
        reply.setParentId(commentId);
        reply = commentRepository.save(reply);
        return commentMapper.toDto(reply);
    }*/

}
