package com.hyetaekon.hyetaekon.searchHistory.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "searchHistory", timeToLive = 2592000) // 30일 유지
public class SearchHistory implements Serializable {
    @Id
    private String id; // userId:timestamp 형태로 구성

    @Indexed // 인덱싱으로 특정 사용자의 검색 기록 조회 가능
    private Long userId;

    private String searchTerm;
    private LocalDateTime createdAt;

    // 팩토리 메서드
    public static SearchHistory of(Long userId, String searchTerm) {
        String id = userId + ":" + System.currentTimeMillis();
        return SearchHistory.builder()
            .id(id)
            .userId(userId)
            .searchTerm(searchTerm)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
