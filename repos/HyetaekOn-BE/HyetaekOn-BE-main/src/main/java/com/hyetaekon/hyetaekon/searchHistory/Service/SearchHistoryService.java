package com.hyetaekon.hyetaekon.searchHistory.Service;

import com.hyetaekon.hyetaekon.searchHistory.Dto.SearchHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryService {
    private final RedisTemplate<String, String> redisTemplate;

    // Redis 키 관련 상수
    private static final String KEY_PREFIX = "searchHistory:";
    private static final int MAX_HISTORY_COUNT = 6;
    private static final int TTL_DAYS = 60;

    /**
     * 검색 기록 저장
     */
    public void saveSearchHistory(Long userId, String searchTerm) {
        if (userId == null || userId == 0L || searchTerm == null || searchTerm.trim().isEmpty()) {
            log.debug("검색 기록 저장 건너뜀: 유효하지 않은 매개변수");
            return;
        }

        try {
            String key = generateKey(userId);
            ListOperations<String, String> listOps = redisTemplate.opsForList();

            // 중복 검색어 제거
            listOps.remove(key, 0, searchTerm);

            // 새 검색어 추가 (왼쪽에서부터 = 최신 순)
            listOps.leftPush(key, searchTerm);

            // 최대 6개로 제한
            listOps.trim(key, 0, MAX_HISTORY_COUNT - 1);

            // 만료 시간 설정
            redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);

            log.debug("사용자 {} 검색 기록 저장 완료: {}", userId, searchTerm);
        } catch (Exception e) {
            log.error("사용자 {} 검색 기록 저장 중 오류 발생: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 사용자 검색 기록 조회
     */
    public List<SearchHistoryDto> getUserSearchHistories(Long userId) {
        if (userId == null || userId == 0L) {
            return new ArrayList<>();
        }

        try {
            String key = generateKey(userId);
            ListOperations<String, String> listOps = redisTemplate.opsForList();

            // 전체 검색 기록 조회 (최신순)
            List<String> searchTerms = listOps.range(key, 0, -1);
            if (searchTerms == null || searchTerms.isEmpty()) {
                return new ArrayList<>();
            }

            // DTO로 변환 (ID는 검색어의 해시값 사용)
            List<SearchHistoryDto> result = new ArrayList<>();
            for (int i = 0; i < searchTerms.size(); i++) {
                String searchTerm = searchTerms.get(i);
                // 고유 ID 생성: 검색어 자체의 해시코드 사용 (인덱스 추가로 동일 검색어 구분)
                String id = String.valueOf(userId + "_" + i + "_" + searchTerm.hashCode());

                result.add(SearchHistoryDto.builder()
                    .id(id)
                    .searchTerm(searchTerm)
                    .createdAt(LocalDateTime.now().minusMinutes(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build());
            }

            return result;
        } catch (Exception e) {
            log.error("사용자 {} 검색 기록 조회 중 오류 발생: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 특정 검색 기록 삭제 (인덱스 기반)
     */
    public void deleteSearchHistory(Long userId, String historyId) {
        if (userId == null || userId == 0L || historyId == null) {
            return;
        }

        try {
            String key = generateKey(userId);

            // 현재 검색어 목록 가져오기
            List<String> searchTerms = redisTemplate.opsForList().range(key, 0, -1);
            if (searchTerms == null || searchTerms.isEmpty()) {
                return;
            }

            // ID 파싱하여 인덱스와 해시코드 추출
            String[] parts = historyId.split("_");
            if (parts.length < 3) {
                log.warn("잘못된 히스토리 ID 형식: {}", historyId);
                return;
            }

            int index;
            try {
                index = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                log.warn("히스토리 ID에서 인덱스 파싱 실패: {}", historyId);
                return;
            }

            // 인덱스 유효성 검사
            if (index < 0 || index >= searchTerms.size()) {
                log.warn("유효하지 않은 인덱스: {}", index);
                return;
            }

            // 해당 인덱스의 검색어 삭제
            String searchTerm = searchTerms.get(index);
            redisTemplate.opsForList().remove(key, 0, searchTerm);

            log.debug("사용자 {} 검색 기록 삭제 완료, 검색어: {}", userId, searchTerm);
        } catch (Exception e) {
            log.error("사용자 {} 검색 기록 삭제 중 오류 발생: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 사용자 검색 기록 전체 삭제
     */
    public void deleteAllSearchHistories(Long userId) {
        if (userId == null || userId == 0L) {
            return;
        }

        try {
            String key = generateKey(userId);
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.debug("사용자 {} 검색 기록 전체 삭제 완료", userId);
            } else {
                log.warn("사용자 {} 검색 기록 전체 삭제 실패: 키가 존재하지 않음", userId);
            }
        } catch (Exception e) {
            log.error("사용자 {} 검색 기록 전체 삭제 중 오류 발생: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 사용자별 Redis 키 생성
     */
    private String generateKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}