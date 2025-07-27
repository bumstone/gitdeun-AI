package com.hyetaekon.hyetaekon.user.entity;

import lombok.Getter;

@Getter
public enum PointActionType {
    POST_CREATION(20),      // 게시글 작성 (20점)
    FIRST_GREETING_POST_CREATION(100), // 인사 게시글 작성 (100점)
    ANSWER_CREATION(10),    // 답변 작성 (10점)
    ANSWER_ACCEPTED(50);    // 답변이 채택됨 (50점)

    private final int points;

    PointActionType(int points) {
        this.points = points;
    }
}