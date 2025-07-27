package com.hyetaekon.hyetaekon.publicservice.service.mongodb;

import com.hyetaekon.hyetaekon.bookmark.repository.BookmarkRepository;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.dto.mongodb.ServiceSearchResultDto;
import com.hyetaekon.hyetaekon.publicservice.mapper.mongodb.ServiceInfoMapper;
import com.hyetaekon.hyetaekon.publicservice.repository.mongodb.MatchedServiceClient;
import com.hyetaekon.hyetaekon.searchHistory.Service.SearchHistoryService;
import com.hyetaekon.hyetaekon.searchHistory.Dto.SearchHistoryDto;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;
import com.hyetaekon.hyetaekon.userInterest.entity.UserInterest;
import com.hyetaekon.hyetaekon.userInterest.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceMatchedHandler {
    private final MatchedServiceClient matchedServiceClient;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ServiceInfoMapper serviceInfoMapper;
    private final UserInterestRepository userInterestRepository;
    private final IncomeEstimationHandler incomeEstimationHandler;
    private final SearchHistoryService searchHistoryService;
    private final ServiceSearchHandler serviceSearchHandler;

    /**
     * 사용자 맞춤 공공서비스 추천 - 사용자 정보 및 검색 기록 기반
     */
    public List<PublicServiceListResponseDto> getPersonalizedServices(Long userId, int size) {
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자 ID입니다."));

        // 사용자 키워드 조회 (관심사 + 검색 기록) - 중복 제거 및 필터링
        List<String> userKeywords = Stream.concat(
                userInterestRepository.findByUserId(userId).stream()
                    .map(UserInterest::getInterest),
                searchHistoryService.getUserSearchHistories(userId).stream()
                    .map(SearchHistoryDto::getSearchTerm)
            )
            .filter(StringUtils::hasText)
            .distinct()
            .toList();

        // 키워드가 없는 경우 빈 목록 반환
        if (userKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        // 사용자 소득 수준 추정
        String userIncomeLevel = incomeEstimationHandler.determineIncomeLevelFromJob(user.getJob());

        // 사용자 나이 계산
        Integer userAge = serviceSearchHandler.calculateAge(user.getBirthAt());

        // MongoDB 클라이언트를 통한 맞춤 서비스 조회
        ServiceSearchResultDto searchResult = matchedServiceClient.getMatchedServices(
            userKeywords,
            user.getGender(),
            userAge,
            userIncomeLevel,
            user.getJob(),
            size
        );

        // DTO 변환 및 북마크 정보 설정
        return searchResult.getResults().stream()
            .map(serviceInfo -> {
                PublicServiceListResponseDto dto = serviceInfoMapper.toDto(serviceInfo);
                dto.setBookmarked(bookmarkRepository.existsByUserIdAndPublicServiceId(
                    userId, serviceInfo.getPublicServiceId()));
                return dto;
            })
            .collect(Collectors.toList());
    }

}
