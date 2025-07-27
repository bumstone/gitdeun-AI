package com.hyetaekon.hyetaekon.user.entity;

import lombok.Getter;

@Getter
public enum UserLevel {
    QUESTION_MARK("물음표", 0, 99),
    EGG("알", 100, 299),
    CHICK("병아리", 300, 499),
    CHICKEN("닭", 500, 699),
    EAGLE("독수리", 700, 999),
    CLOUD("구름", 1000, Integer.MAX_VALUE);

    private final String name;
    private final int minPoint;
    private final int maxPoint;

    UserLevel(String name, int minPoint, int maxPoint) {
        this.name = name;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
    }

    public static UserLevel fromPoint(int point) {
        for (UserLevel level : values()) {
            if (point >= level.minPoint && point <= level.maxPoint) {
                return level;
            }
        }
        return QUESTION_MARK; // 기본값
    }
}
