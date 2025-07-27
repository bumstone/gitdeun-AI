package com.hyetaekon.hyetaekon.recommend.service;

import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.post.entity.Post;
import com.hyetaekon.hyetaekon.post.repository.PostRepository;
import com.hyetaekon.hyetaekon.recommend.entity.Recommend;
import com.hyetaekon.hyetaekon.recommend.repository.RecommendRepository;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.hyetaekon.hyetaekon.common.exception.ErrorCode.*;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendService {
    private final RecommendRepository recommendRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public void addRecommend(Long postId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(RECOMMEND_USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new GlobalException(POST_NOT_FOUND_BY_ID));

        // 이미 북마크가 있는지 확인
        if (recommendRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new GlobalException(BOOKMARK_ALREADY_EXISTS);
        }

        Recommend recommend = Recommend.builder()
            .user(user)
            .post(post)
            .build();

        recommendRepository.save(recommend);

        // 북마크 수 증가
        post.incrementRecommendCnt();
        postRepository.save(post);
    }

    @jakarta.transaction.Transactional
    public void removeRecommend(Long postId, Long userId) {
        Recommend recommend = recommendRepository.findByUserIdAndPostId(userId, postId)
            .orElseThrow(() -> new GlobalException(RECOMMEND_NOT_FOUND));

        recommendRepository.delete(recommend);

        // 추천수 감소
        Post post  = recommend.getPost();
        post.decrementRecommendCnt();
        postRepository.save(post);
    }
}
