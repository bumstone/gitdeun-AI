package com.hyetaekon.hyetaekon.user.service;

import com.hyetaekon.hyetaekon.common.exception.ErrorCode;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;
import com.hyetaekon.hyetaekon.post.entity.PostType;
import com.hyetaekon.hyetaekon.post.repository.PostRepository;
import com.hyetaekon.hyetaekon.user.entity.PointActionType;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPointService {
    private final UserRepository userRepository;
    private final UserLevelService userLevelService;
    private final PostRepository postRepository;

    @Transactional
    public void addPointForAction(Long userId, PointActionType actionType) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        int pointToAdd = actionType.getPoints();

        // 현재 포인트 로깅
        log.info("포인트 부여 전 - 사용자 ID: {}, 현재 포인트: {}, 부여할 포인트: {}",
            userId, user.getPoint(), pointToAdd);

        user.addPoint(pointToAdd);

        // 레벨 체크 및 업데이트
        userLevelService.checkAndUpdateLevel(user);

        User savedUser = userRepository.save(user);

        // 포인트 부여 후 로깅
        log.info("포인트 부여 후 - 사용자 ID: {}, 최종 포인트: {}, 레벨: {}",
            userId, savedUser.getPoint(), savedUser.getLevel().getName());
    }

}
