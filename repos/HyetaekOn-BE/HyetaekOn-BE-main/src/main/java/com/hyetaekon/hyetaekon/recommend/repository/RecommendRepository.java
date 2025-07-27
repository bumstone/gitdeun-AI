package com.hyetaekon.hyetaekon.recommend.repository;

import com.hyetaekon.hyetaekon.recommend.entity.Recommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendRepository extends JpaRepository<Recommend, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Recommend> findByUserIdAndPostId(Long userId, Long postId);

}
