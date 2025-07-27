package com.hyetaekon.hyetaekon.publicservice.entity.mongodb;

public enum IncomeLevel {
    VERY_LOW("0-50%", "LOW"),
    LOW("51-75%", "MIDDLE_LOW"),
    MEDIUM("76-100%", "MIDDLE"),
    HIGH("101-200%", "MIDDLE_HIGH"),
    VERY_HIGH("200%+", "HIGH"),
    ANY("ANY", "ANY");  // 모든 소득수준 허용

    private final String percentageRange;
    private final String code;

    IncomeLevel(String percentageRange, String code) {
        this.percentageRange = percentageRange;
        this.code = code;
    }

    // 퍼센트 범위에서 IncomeLevel 찾기
    public static IncomeLevel findByPercentageRange(String percentageRange) {
        for (IncomeLevel level : values()) {
            if (level.percentageRange.equals(percentageRange)) {
                return level;
            }
        }
        return null;
    }

    // 코드에서 IncomeLevel 찾기
    public static IncomeLevel findByCode(String code) {
        for (IncomeLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
