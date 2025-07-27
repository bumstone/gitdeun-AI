package com.hyetaekon.hyetaekon.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.hyetaekon.hyetaekon.publicservice.entity.CacheType;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 각 캐시 타입에 대한 설정 등록
        Arrays.stream(CacheType.values())
            .forEach(cacheType -> {
                cacheManager.registerCustomCache(cacheType.getCacheName(),
                    Caffeine.newBuilder()
                        .recordStats()   // 캐시 통계 기록
                        .expireAfterWrite(Duration.ofHours(cacheType.getExpiredAfterWrite()))  // 항목 만료 시간
                        .maximumSize(cacheType.getMaximumSize())  // 최대 크기
                        .build()
                );
            });

        return cacheManager;
    }
}
