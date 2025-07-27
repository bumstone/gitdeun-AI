package com.hyetaekon.hyetaekon.publicservice.service.mongodb;

import com.hyetaekon.hyetaekon.searchHistory.Service.SearchHistoryService;
import com.hyetaekon.hyetaekon.userInterest.entity.UserInterest;
import com.hyetaekon.hyetaekon.userInterest.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.hyetaekon.hyetaekon.bookmark.repository.BookmarkRepository;
import com.hyetaekon.hyetaekon.publicservice.dto.PublicServiceListResponseDto;
import com.hyetaekon.hyetaekon.publicservice.dto.mongodb.ServiceSearchCriteriaDto;
import com.hyetaekon.hyetaekon.publicservice.dto.mongodb.ServiceSearchResultDto;
import com.hyetaekon.hyetaekon.publicservice.repository.mongodb.ServiceSearchClient;
import com.hyetaekon.hyetaekon.publicservice.mapper.mongodb.ServiceInfoMapper;
import com.hyetaekon.hyetaekon.user.entity.User;
import com.hyetaekon.hyetaekon.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceSearchHandler {
    private final ServiceSearchClient serviceSearchClient;
    private final BookmarkRepository bookmarkRepository;
    private final ServiceInfoMapper serviceInfoMapper;
    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final IncomeEstimationHandler incomeEstimationHandler;
    private final SearchHistoryService searchHistoryService;

    // 기본 검색 (비로그인)
    public Page<PublicServiceListResponseDto> searchServices(ServiceSearchCriteriaDto criteria) {
        // 검색 조건이 없는 경우 빈 결과 반환
        if (!StringUtils.hasText(criteria.getSearchTerm())) {
            return Page.empty(criteria.getPageable());
        }

        // MongoDB 검색 수행
        ServiceSearchResultDto searchResult = serviceSearchClient.search(criteria);

        // 검색 결과를 DTO로 변환 (북마크 정보 없이)
        return convertToPageResponse(searchResult, null);
    }

    // 맞춤 검색 (로그인)
    public Page<PublicServiceListResponseDto> searchPersonalizedServices(
        ServiceSearchCriteriaDto criteria, Long userId) {

        // 검색 조건이 없는 경우 빈 결과 반환
        if (!StringUtils.hasText(criteria.getSearchTerm())) {
            return Page.empty(criteria.getPageable());
        } else if(StringUtils.hasText(criteria.getSearchTerm())) { // 검색어가 유효하면 검색 기록 저장
            searchHistoryService.saveSearchHistory(userId, criteria.getSearchTerm());
        }

        // 사용자 정보 가져오기
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자 ID입니다."));

        String userIncomeLevel = incomeEstimationHandler.determineIncomeLevelFromJob(user.getJob());

        // 사용자 관심사 목록 추출
        List<String> userInterests = userInterestRepository.findByUserId(userId).stream()
            .map(UserInterest::getInterest)
            .collect(Collectors.toList());

        // 사용자 정보로 검색 조건 보강
        ServiceSearchCriteriaDto enrichedCriteria = criteria.withUserInfo(
            userInterests,
            user.getGender(),
            calculateAge(user.getBirthAt()),
            userIncomeLevel,
            user.getJob()
        );

        // MongoDB 검색 수행
        ServiceSearchResultDto searchResult = serviceSearchClient.search(enrichedCriteria);

        // 검색 결과를 DTO로 변환 (북마크 정보 포함)
        return convertToPageResponse(searchResult, userId);
    }

    private Page<PublicServiceListResponseDto> convertToPageResponse(
        ServiceSearchResultDto searchResult, Long userId) {

        List<PublicServiceListResponseDto> dtoList = searchResult.getResults().stream()
            .map(serviceInfo -> {
                // Entity를 DTO로 변환
                PublicServiceListResponseDto dto = serviceInfoMapper.toDto(serviceInfo);

                // 사용자 ID가 제공된 경우 북마크 정보 설정
                if (userId != null) {
                    dto.setBookmarked(bookmarkRepository.existsByUserIdAndPublicServiceId(
                        userId, serviceInfo.getPublicServiceId()));
                }

                return dto;
            })
            .collect(Collectors.toList());

        return new PageImpl<>(
            dtoList,
            PageRequest.of(searchResult.getCurrentPage(),
                searchResult.getResults().isEmpty() ? 10 : searchResult.getResults().size()),
            searchResult.getTotal()
        );
    }

    // 나이 계산 헬퍼 메서드
    public Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    // 자동완성 기능 - 캐싱 적용
    @Cacheable(value = "serviceAutocomplete", key = "#word", unless = "#result.isEmpty()")
    public List<String> getAutocompleteResults(String word) {
        if (!StringUtils.hasText(word) || word.length() < 2) {
            return new ArrayList<>();
        }
        return serviceSearchClient.getAutocompleteResults(word);
    }

    // 자동완성 캐시 무효화
    @CacheEvict(value = "serviceAutocomplete", allEntries = true)
    public void refreshAutocompleteCache() {
    }

}
