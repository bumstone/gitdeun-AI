package com.hyetaekon.hyetaekon.post.repository;

import com.hyetaekon.hyetaekon.post.entity.Post;
import com.hyetaekon.hyetaekon.post.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 삭제되지 않은 모든 게시글 조회 (페이징)
    Page<Post> findByDeletedAtIsNull(Pageable pageable);

    // 특정 타입의 삭제되지 않은 게시글 조회 (페이징)
    Page<Post> findByPostTypeAndDeletedAtIsNull(PostType postType, Pageable pageable);

    // ID로 삭제되지 않은 게시글 조회
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    // 특정 사용자가 특정 타입의 게시글을 작성한 적이 있는지 확인
    boolean existsByUser_IdAndPostTypeAndDeletedAtIsNull(Long userId, PostType postType);

    // 특정 사용자가 작성한 게시글 조회
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p " +
        "WHERE p.user.id = :userId " +
        "AND p.deletedAt IS NULL " +
        "ORDER BY p.createdAt DESC")
    Page<Post> findMyPostsOptimized(@Param("userId") Long userId, Pageable pageable);

    // 특정 사용자가 추천한 게시글 조회
    @Query("SELECT DISTINCT p FROM Post p " +
        "JOIN p.recommends r " +
        "WHERE r.user.id = :userId " +
        "AND p.deletedAt IS NULL " +
        "ORDER BY r.createdAt DESC")
    Page<Post> findRecommendedPostsOptimized(@Param("userId") Long userId, Pageable pageable);

    // 제목 검색 + 삭제되지 않은 게시글
    Page<Post> findByTitleContainingAndDeletedAtIsNull(String keyword, Pageable pageable);

    // 제목 검색 + 특정 타입 + 삭제되지 않은 게시글
    Page<Post> findByPostTypeAndTitleContainingAndDeletedAtIsNull(PostType postType, String keyword, Pageable pageable);

    // 삭제된 게시글만 조회 (관리자용)
    Page<Post> findByDeletedAtIsNotNull(Pageable pageable);

    // 정지된 게시글만 조회 (관리자용)
    Page<Post> findBySuspendAtIsNotNull(Pageable pageable);


    // 통계용 카운팅 메서드들
    long count();
    long countByDeletedAtIsNotNull();
    long countBySuspendAtIsNotNull();

}
