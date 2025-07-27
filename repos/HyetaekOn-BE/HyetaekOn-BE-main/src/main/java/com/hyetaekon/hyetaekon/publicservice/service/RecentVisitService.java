package com.hyetaekon.hyetaekon.publicservice.service;

import com.hyetaekon.hyetaekon.bookmark.repository.BookmarkRepository;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.entity.PublicService;
import com.hyetaekon.hyetaekon.publicservice.mapper.PublicServiceMapper;
import com.hyetaekon.hyetaekon.publicservice.repository.PublicServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecentVisitService {

    private final RedisTemplate<String, String> redisTemplate;
    private final PublicServiceRepository publicServiceRepository;
    private final PublicServiceMapper publicServiceMapper;
    private final BookmarkRepository bookmarkRepository;

    // Redis 키 형식: "user:{userId}:recentServices"
    private static final String KEY_PREFIX = "user:";
    private static final String KEY_SUFFIX = ":recentServices";

    // 설정값
    private static final int MAX_ITEMS = 10;        // 최대 저장 개수
    private static final int TTL_DAYS = 30;         // 데이터 유지 기간(일)

    /**
     * 사용자의 서비스 방문 기록 추가
     */
    public void addVisit(Long userId, String serviceId) {
        if (userId == null || userId == 0 || serviceId == null) {
            return; // 비로그인 사용자나 유효하지 않은 ID는 처리하지 않음
        }

        String key = generateKey(userId);

        // 이미 존재하는 경우 삭제 (중복 제거)
        redisTemplate.opsForList().remove(key, 0, serviceId);

        // 맨 앞에 새로 추가 (최신 방문)
        redisTemplate.opsForList().leftPush(key, serviceId);

        // 최대 10개로 제한
        redisTemplate.opsForList().trim(key, 0, MAX_ITEMS - 1);

        // TTL 설정 (30일)
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);

        log.debug("사용자 {} - 최근 방문 서비스 추가: {}", userId, serviceId);
    }

    /**
     * 최근 방문 서비스 목록 조회 (페이징)
     * - 서비스 ID 목록: Redis에서 조회
     * - 서비스 기본 정보: Caffeine 캐시 활용 (변경이 적은 정보)
     * - 북마크 상태: 직접 DB 조회 (자주 변경되는 정보)
     */
    public Page<PublicServiceListResponseDto> getRecentVisits(Long userId, int page, int size) {
        String key = generateKey(userId);

        // 전체 개수 확인
        Long total = redisTemplate.opsForList().size(key);
        if (total == null || total == 0) {
            return Page.empty(PageRequest.of(page, size));
        }

        // 현재 페이지에 해당하는 범위 계산
        int start = page * size;
        int end = start + size - 1;

        // Redis에서 ID 목록 조회
        List<String> serviceIds = redisTemplate.opsForList().range(key, start, end);
        if (serviceIds == null || serviceIds.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }

        // 서비스 정보 조회 (ID별로 분리하여 처리)
        List<PublicService> services = new ArrayList<>();
        for (String id : serviceIds) {
            // 기본 정보는 캐시 활용 (캐시 실패 시 직접 조회)
            PublicService service = getServiceBasicInfo(id);
            if (service != null) {
                services.add(service);
            }
        }

        // DTO 변환 및 북마크 정보 설정
        List<PublicServiceListResponseDto> dtoList = services.stream()
            .map(service -> {
                // 기본 정보 변환
                PublicServiceListResponseDto dto = publicServiceMapper.toListDto(service);

                // 북마크 상태는 항상 직접 조회 (자주 변경되는 정보)
                if (userId != 0L) {
                    dto.setBookmarked(isBookmarked(userId, service.getId()));
                }

                return dto;
            })
            .collect(Collectors.toList());

        // 페이지 객체 생성 및 반환
        return new PageImpl<>(dtoList, PageRequest.of(page, size), total);
    }

    /**
     * 서비스 기본 정보 조회 (캐시 적용)
     * - 서비스 이름, 설명 등 자주 변경되지 않는 정보에 캐시 적용
     */
    @Cacheable(value = "serviceBasicInfo", key = "#serviceId", unless = "#result == null")
    public PublicService getServiceBasicInfo(String serviceId) {
        log.debug("Cache miss: 서비스 기본 정보 DB 조회 - ID: {}", serviceId);
        return publicServiceRepository.findById(serviceId).orElse(null);
    }

    /**
     * 북마크 상태 조회 (항상 직접 DB 조회)
     * - 자주 변경될 수 있어 캐시 미적용
     */
    public boolean isBookmarked(Long userId, String serviceId) {
        return bookmarkRepository.existsByUserIdAndPublicServiceId(userId, serviceId);
    }

    /**
     * 특정 사용자의 전체 방문 기록 삭제
     */
    public void clearUserVisits(Long userId) {
        String key = generateKey(userId);
        redisTemplate.delete(key);
        log.debug("사용자 {} - 방문 기록 전체 삭제", userId);
    }

    /**
     * Redis 키 생성
     */
    private String generateKey(Long userId) {
        return KEY_PREFIX + userId + KEY_SUFFIX;
    }
}