package com.hyetaekon.hyetaekon.comment.repository;

import com.hyetaekon.hyetaekon.comment.entity.Comment;
import com.hyetaekon.hyetaekon.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 게시글의 최상위 댓글 조회 (삭제되지 않은 댓글만)
    Page<Comment> findByPostAndParentIdIsNull(Post post, Pageable pageable);

    // 특정 댓글의 대댓글 조회 (삭제되지 않은 댓글만)
    Page<Comment> findByPostAndParentId(Post post, Long parentId, Pageable pageable);

    // 삭제된 댓글만 조회 (관리자용)
    Page<Comment> findByPostAndDeletedAtIsNotNull(Post post, Pageable pageable);

    // 정지된 댓글만 조회 (관리자용)
    Page<Comment> findByPostAndSuspendAtIsNotNull(Post post, Pageable pageable);

    // 통계용 카운팅 메서드들
    long countByPost(Post post);
    long countByPostAndDeletedAtIsNotNull(Post post);
    long countByPostAndSuspendAtIsNotNull(Post post);
}
