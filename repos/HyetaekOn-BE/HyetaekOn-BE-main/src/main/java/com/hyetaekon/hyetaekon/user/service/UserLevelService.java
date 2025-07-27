package com.hyetaekon.hyetaekon.user.service;

import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.entity.UserLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLevelService {

    @Transactional
    public void checkAndUpdateLevel(User user) {
        int currentPoint = user.getPoint();
        UserLevel appropriateLevel = UserLevel.fromPoint(currentPoint);

        // 현재 레벨과 적절한 레벨이 다른 경우에만 업데이트
        if (appropriateLevel != user.getLevel()) {
            user.updateLevel(appropriateLevel);
            // 레벨 업 이벤트 발행 가능 (옵션)
        }
    }

}
