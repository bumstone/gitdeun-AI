package com.hyetaekon.hyetaekon.answer.repository;

import com.hyetaekon.hyetaekon.answer.entity.Answer;
import com.hyetaekon.hyetaekon.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    // 페이지네이션 적용한 답변 목록 조회 (post 객체 사용)
    Page<Answer> findByPost(Post post, Pageable pageable);

    // 채택 여부 및 등록일 기준 정렬된 페이징 처리된 답변 목록 조회
    Page<Answer> findByPostOrderBySelectedDescCreatedAtDesc(Post post, Pageable pageable);

    // 삭제된 답변만 조회 (관리자용)
    Page<Answer> findByPostAndDeletedAtIsNotNull(Post post, Pageable pageable);

    // 정지된 답변만 조회 (관리자용)
    Page<Answer> findByPostAndSuspendAtIsNotNull(Post post, Pageable pageable);

    // 통계용 카운팅 메서드들
    long countByPost(Post post);
    long countByPostAndDeletedAtIsNotNull(Post post);
    long countByPostAndSuspendAtIsNotNull(Post post);
}
