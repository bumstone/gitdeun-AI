package com.hyetaekon.hyetaekon.post.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostType {
    ALL("전체"),
    QUESTION("질문"),
    FREE("자유"),
    GREETING("인사");

    @JsonValue
    private final String koreanName;

    /**
     * 한글 이름으로 PostType을 찾습니다.
     */
    public static PostType fromKoreanName(String koreanName) {
        for (PostType type : values()) {
            if (type.getKoreanName().equals(koreanName)) {
                return type;
            }
        }
        // 일치하는 이름이 없거나 null인 경우 기본값으로 ALL 반환
        return ALL;
    }

    /**
     * 클라이언트에서 영문 타입 코드로 전송된 PostType을 찾습니다.
     */
    public static PostType fromString(String typeCode) {
        if (typeCode == null || typeCode.trim().isEmpty()) {
            return ALL; // 기본값
        }

        try {
            return PostType.valueOf(typeCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
