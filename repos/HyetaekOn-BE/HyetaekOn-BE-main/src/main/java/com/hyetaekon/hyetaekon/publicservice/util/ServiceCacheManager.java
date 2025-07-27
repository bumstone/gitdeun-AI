package com.hyetaekon.hyetaekon.publicservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCacheManager {

    private final CacheManager cacheManager;

    /**
     * 특정 서비스의 기본 정보 캐시 삭제
     */
    @CacheEvict(value = "serviceBasicInfo", key = "#serviceId")
    public void evictServiceBasicInfoCache(String serviceId) {
        log.debug("서비스 기본 정보 캐시 삭제: {}", serviceId);
    }

    /**
     * 서비스 데이터 동기화 시 캐시 일괄 삭제
     */
    public void clearAllServiceCaches() {
        cacheManager.getCache("serviceBasicInfo").clear();
        log.info("모든 서비스 기본 정보 캐시 삭제 완료");
    }
}
