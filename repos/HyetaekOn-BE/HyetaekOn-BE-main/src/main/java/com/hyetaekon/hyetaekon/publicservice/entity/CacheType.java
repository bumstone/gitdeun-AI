package com.hyetaekon.hyetaekon.publicservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    SERVICE_AUTOCOMPLETE("serviceAutocomplete", 2, 1200),   // 자동완성 캐시
    FILTER_OPTIONS("filterOptions", 24, 50),              // 필터 옵션 캐시 (하루 유지)
    SERVICE_BASIC_INFO("serviceBasicInfo", 12, 1000);     // 서비스 기본 정보 캐시

    private final String cacheName;
    private final int expiredAfterWrite;  // 시간(hour) 단위
    private final int maximumSize;        // 최대 캐시 항목 수
}